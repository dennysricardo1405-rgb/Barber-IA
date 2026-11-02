package com.example.BarberiaLaClasica.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.BarberiaLaClasica.model.Barbero;
import com.example.BarberiaLaClasica.model.BotConversacion;
import com.example.BarberiaLaClasica.model.BotEvento;
import com.example.BarberiaLaClasica.model.Cita;
import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.model.Servicio;
import com.example.BarberiaLaClasica.repository.BarberoRepository;
import com.example.BarberiaLaClasica.repository.BotConversacionRepository;
import com.example.BarberiaLaClasica.repository.BotEventoRepository;
import com.example.BarberiaLaClasica.repository.CitaRepository;
import com.example.BarberiaLaClasica.repository.ClienteRepository;
import com.example.BarberiaLaClasica.repository.ServicioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * U4 - Reglas del agente de citas. La IA solo conversa: todo lo que se guarda pasa por aquí y se
 * valida contra la base de datos, aunque el modelo se equivoque.
 */
@Service
public class BotAgendaService {

    private static final List<Integer> ESTADOS_ACTIVOS = List.of(1, 2);
    private static final String[] DIAS = { "LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO" };

    private final BotConversacionRepository conversaciones;
    private final BotEventoRepository eventos;
    private final ClienteRepository clientes;
    private final ServicioRepository servicios;
    private final BarberoRepository barberos;
    private final CitaRepository citas;
    private final ClienteService clienteService;
    private final DniService dniService;
    private final PromocionHelper promocionHelper;
    private final ObjectMapper json = new ObjectMapper();
    /** Evita que dos chats tomen el mismo horario al mismo tiempo. */
    private final ReentrantLock candadoReserva = new ReentrantLock();

    private final LocalTime apertura;
    private final LocalTime cierre;
    private final int intervaloMin;
    private final int diasAnticipacion;
    private final int minutosAbandono;
    private final Clock reloj;

    public BotAgendaService(BotConversacionRepository conversaciones, BotEventoRepository eventos,
            ClienteRepository clientes, ServicioRepository servicios, BarberoRepository barberos,
            CitaRepository citas, ClienteService clienteService, DniService dniService,
            PromocionHelper promocionHelper,
            @Value("${bot.horario.apertura:10:00}") String apertura,
            @Value("${bot.horario.cierre:22:00}") String cierre,
            @Value("${bot.horario.intervalo-min:30}") int intervaloMin,
            @Value("${bot.dias-anticipacion:30}") int diasAnticipacion,
            @Value("${bot.minutos-abandono:30}") int minutosAbandono,
            @Value("${bot.zona-horaria:America/Lima}") String zona) {
        this.conversaciones = conversaciones;
        this.eventos = eventos;
        this.clientes = clientes;
        this.servicios = servicios;
        this.barberos = barberos;
        this.citas = citas;
        this.clienteService = clienteService;
        this.dniService = dniService;
        this.promocionHelper = promocionHelper;
        this.apertura = LocalTime.parse(apertura);
        this.cierre = LocalTime.parse(cierre);
        this.intervaloMin = intervaloMin;
        this.diasAnticipacion = diasAnticipacion;
        this.minutosAbandono = minutosAbandono;
        this.reloj = Clock.system(ZoneId.of(zona));
    }

    public LocalDateTime ahora() {
        return LocalDateTime.now(reloj).withNano(0);
    }

    // ── Conversación ────────────────────────────────────────────────────────

    /** Registra un mensaje entrante y devuelve la conversación abierta (o una nueva). */
    @Transactional
    public Map<String, Object> registrarMensaje(String chatId, String canal) {
        BotConversacion c = conversacionAbierta(chatId, canal);
        c.setMensajes(c.getMensajes() + 1);
        conversaciones.save(c);

        Map<String, Object> r = ok();
        r.put("conversacionId", c.getId());
        r.put("fechaHoraActual", ahora().toString());
        Optional<Cliente> conocido = clientes.findFirstByTelegramChatId(chatId);
        r.put("clienteConocido", conocido.isPresent());
        conocido.ifPresent(cl -> {
            r.put("dni", cl.getDni());
            r.put("nombre", cl.getNombres());
        });
        return r;
    }

    private BotConversacion conversacionAbierta(String chatId, String canal) {
        if (chatId == null || chatId.isBlank()) {
            throw new BotException("CHAT_INVALIDO", "Falta el identificador del chat.");
        }
        LocalDateTime ahora = ahora();
        Optional<BotConversacion> ultima = conversaciones.findFirstByChatIdOrderByUltimaActividadDesc(chatId);
        // Se sigue la misma conversación mientras haya actividad reciente (también después de crear la cita,
        // para que un "gracias" o un doble "sí" no cuenten como conversaciones nuevas)
        if (ultima.isPresent() && ultima.get().getUltimaActividad().isAfter(ahora.minusMinutes(minutosAbandono))) {
            BotConversacion c = ultima.get();
            c.setUltimaActividad(ahora);
            return c;
        }
        BotConversacion c = new BotConversacion();
        c.setChatId(chatId);
        c.setCanal(canal == null || canal.isBlank() ? "TELEGRAM" : canal.toUpperCase());
        c.setInicio(ahora);
        c.setUltimaActividad(ahora);
        return conversaciones.save(c);
    }

    // ── Cliente ─────────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> buscarCliente(String chatId, String dniTexto) {
        BotConversacion conv = conversacionAbierta(chatId, null);
        String dni = validarDni(dniTexto);
        conv.setDni(dni);

        Map<String, Object> r = ok();
        r.put("dni", dni);
        Optional<Cliente> encontrado = clientes.findByDni(dni);
        if (encontrado.isPresent()) {
            Cliente cl = encontrado.get();
            if (cl.getEstado() != 1) {
                throw new BotException("CLIENTE_INACTIVO",
                        "Esa cuenta está desactivada. Comunícate con la barbería para reactivarla.");
            }
            cl.setTelegramChatId(chatId);
            clientes.save(cl);
            r.put("existe", true);
            r.put("nombre", cl.getNombres() + " " + cl.getApellidos());
            r.put("citasActivas", citasActivas(cl));
        } else {
            r.put("existe", false);
            consultarReniec(dni).ifPresent(n -> {
                r.put("nombresReniec", n[0]);
                r.put("apellidosReniec", n[1]);
            });
            r.put("siguientePaso", "Pedir el número de celular (9 dígitos) y confirmar el nombre para registrarlo.");
        }
        conversaciones.save(conv);
        return r;
    }

    @Transactional
    public Map<String, Object> registrarCliente(String chatId, String dniTexto, String nombres, String apellidos,
            String telefono) {
        BotConversacion conv = conversacionAbierta(chatId, null);
        String dni = validarDni(dniTexto);
        if (clientes.existsByDni(dni)) {
            throw new BotException("CLIENTE_YA_EXISTE", "Ese DNI ya está registrado; usa buscar_cliente.");
        }
        String tel = telefono == null ? "" : telefono.replaceAll("[\\s-]", "").replaceFirst("^\\+?51", "");
        if (!tel.matches("9\\d{8}")) {
            throw new BotException("TELEFONO_INVALIDO", "El celular debe tener 9 dígitos y empezar con 9.");
        }
        String n = limpiar(nombres);
        String a = limpiar(apellidos);
        if (n.isEmpty() || a.isEmpty()) {
            Optional<String[]> reniec = consultarReniec(dni);
            if (reniec.isPresent()) {
                n = n.isEmpty() ? reniec.get()[0] : n;
                a = a.isEmpty() ? reniec.get()[1] : a;
            }
        }
        if (n.isEmpty() || a.isEmpty()) {
            throw new BotException("FALTAN_DATOS", "Necesito el nombre y apellido del cliente para registrarlo.");
        }
        Cliente c = new Cliente();
        c.setDni(dni);
        c.setNombres(n);
        c.setApellidos(a);
        c.setTelefono(tel);
        c.setTelegramChatId(chatId);
        // Misma contraseña inicial que el registro rápido de recepción
        Cliente guardado = clienteService.crearDesdeAdmin(c, "B" + dni);
        conv.setDni(dni);
        conversaciones.save(conv);

        Map<String, Object> r = ok();
        r.put("registrado", true);
        r.put("nombre", guardado.getNombres() + " " + guardado.getApellidos());
        return r;
    }

    // ── Catálogo y horarios ─────────────────────────────────────────────────

    public Map<String, Object> catalogo() {
        List<Map<String, Object>> listaServicios = new ArrayList<>();
        for (Servicio s : servicios.findByEstado(1)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("nombre", s.getNombre());
            m.put("precio", promocionHelper.calcularPrecioServicio(s));
            m.put("duracionMinutos", s.getDuracionMinutos());
            listaServicios.add(m);
        }
        List<Map<String, Object>> listaBarberos = new ArrayList<>();
        for (Barbero b : barberos.findByEstado(1)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("nombre", b.getNombre());
            m.put("especialidad", b.getEspecialidad());
            m.put("diaLibre", b.getDiaLibre());
            listaBarberos.add(m);
        }
        Map<String, Object> r = ok();
        r.put("servicios", listaServicios);
        r.put("barberos", listaBarberos);
        r.put("horarioAtencion", apertura + " a " + cierre);
        return r;
    }

    @Transactional
    public Map<String, Object> horarios(String chatId, Long barberoId, String fechaTexto, Long servicioId) {
        return horarios(chatId, barberoId, fechaTexto, servicioId, null);
    }

    /** Si viene la hora que pidió el cliente, también se valida (horario de atención, hora pasada, ocupada). */
    public Map<String, Object> horarios(String chatId, Long barberoId, String fechaTexto, Long servicioId,
            String horaTexto) {
        BotConversacion conv = conversacionAbierta(chatId, null);
        conversaciones.save(conv);
        Barbero barbero = barberoActivo(barberoId);
        LocalDate fecha = validarFecha(fechaTexto, barbero);
        int duracion = servicioId == null ? intervaloMin : servicioActivo(servicioId).getDuracionMinutos();

        if (horaTexto != null && !horaTexto.isBlank()) {
            LocalTime hora = validarHora(horaTexto, fecha, duracion);
            if (citas.existeConflictoHorario(barbero.getId(), fecha, hora, hora.plusMinutes(duracion))) {
                throw new BotException("HORARIO_OCUPADO", "Esa hora ya está tomada con ese barbero.",
                        Map.of("alternativas", alternativas(barbero.getId(), fecha, duracion, hora)));
            }
        }

        List<String> libres = horasLibres(barbero.getId(), fecha, duracion);
        Map<String, Object> r = ok();
        r.put("barbero", barbero.getNombre());
        r.put("fecha", fecha.toString());
        r.put("diaSemana", DIAS[fecha.getDayOfWeek().getValue() - 1]);
        r.put("horasLibres", libres);
        if (libres.isEmpty()) {
            r.put("mensaje", "No quedan horarios libres ese día con ese barbero. Ofrece otro día u otro barbero.");
        }
        return r;
    }

    // ── Crear la cita ───────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> crearCita(String chatId, String dniTexto, Long servicioId, Long barberoId,
            String fechaTexto, String horaTexto, Boolean confirmado) {
        BotConversacion conv = conversacionAbierta(chatId, null);

        // Idempotencia: si esta conversación ya creó su cita (doble "sí", reintento de n8n), se devuelve la misma
        if (conv.getCitaId() != null) {
            Cita existente = citas.findById(conv.getCitaId()).orElse(null);
            if (existente != null) {
                Map<String, Object> r = comprobante(existente);
                r.put("yaRegistrada", true);
                return r;
            }
        }
        if (!Boolean.TRUE.equals(confirmado)) {
            throw new BotException("SIN_CONFIRMACION",
                    "Antes de registrar, muestra el resumen al cliente y espera que responda que sí.");
        }
        String dni = validarDni(dniTexto);
        Cliente cliente = clientes.findByDni(dni).orElseThrow(() -> new BotException("CLIENTE_NO_REGISTRADO",
                "Ese DNI no está registrado. Primero registra al cliente."));
        Servicio servicio = servicioActivo(servicioId);
        Barbero barbero = barberoActivo(barberoId);
        LocalDate fecha = validarFecha(fechaTexto, barbero);
        LocalTime hora = validarHora(horaTexto, fecha, servicio.getDuracionMinutos());

        candadoReserva.lock();
        try {
            List<Map<String, Object>> activas = citasActivas(cliente);
            if (!activas.isEmpty()) {
                throw new BotException("CITA_ACTIVA_EXISTENTE",
                        "El cliente ya tiene una cita pendiente; solo se permite una a la vez.",
                        Map.of("citasActivas", activas));
            }
            LocalTime fin = hora.plusMinutes(servicio.getDuracionMinutos());
            if (citas.existeConflictoHorario(barbero.getId(), fecha, hora, fin)) {
                throw new BotException("HORARIO_OCUPADO", "Ese horario acaba de ocuparse.",
                        Map.of("alternativas", alternativas(barbero.getId(), fecha, servicio.getDuracionMinutos(), hora)));
            }

            cliente.setTotalVisitas(clienteService.calcularTotalVisitas(cliente));
            Cita cita = new Cita();
            cita.setCliente(cliente);
            cita.setBarbero(barbero);
            cita.setServicio(servicio);
            cita.setFecha(fecha);
            cita.setHoraInicio(hora);
            cita.setHoraFin(fin);
            cita.setTotalPrecio(BigDecimal.valueOf(promocionHelper.calcularPrecioServicio(servicio, cliente)));
            cita.setMontoYape(BigDecimal.ZERO);
            cita.setMontoEfectivo(BigDecimal.ZERO);
            cita.setEstado(1); // PENDIENTE: aparece en Citas/Recepción como las de la web
            cita.setOrigen("BOT");
            cita = citas.save(cita);

            if (cliente.getTelegramChatId() == null) {
                cliente.setTelegramChatId(chatId);
                clientes.save(cliente);
            }
            conv.setDni(dni);
            conv.setCitaId(cita.getId());
            conv.setResultado(BotConversacion.CITA_CREADA);
            conv.setFin(ahora());
            conversaciones.save(conv);
            return comprobante(cita);
        } finally {
            candadoReserva.unlock();
        }
    }

    // ── Bitácora de excepciones ─────────────────────────────────────────────

    /** Guarda un error controlado. Va en su propia transacción para que quede aunque la operación se revierta. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarErrorControlado(String chatId, BotException e) {
        Long conversacionId = null;
        if (chatId != null && !chatId.isBlank()) {
            Optional<BotConversacion> c = conversaciones.findFirstByChatIdOrderByUltimaActividadDesc(chatId);
            if (c.isPresent()) {
                c.get().setErroresControlados(c.get().getErroresControlados() + 1);
                conversaciones.save(c.get());
                conversacionId = c.get().getId();
            }
        }
        guardarEvento(conversacionId, BotEvento.ERROR_CONTROLADO, e.getCodigo(), e.getMessage());
    }

    /** Fallo no previsto reportado por el flujo de errores de n8n. */
    @Transactional
    public void registrarFallo(String flujo, String nodo, String mensaje) {
        String detalle = String.format("[%s / %s] %s", nvl(flujo), nvl(nodo), nvl(mensaje));
        guardarEvento(null, BotEvento.FALLO, "FALLO_N8N", detalle);
    }

    private void guardarEvento(Long conversacionId, String tipo, String codigo, String detalle) {
        BotEvento ev = new BotEvento();
        ev.setConversacionId(conversacionId);
        ev.setFecha(ahora());
        ev.setTipo(tipo);
        ev.setCodigo(codigo.length() > 40 ? codigo.substring(0, 40) : codigo);
        ev.setDetalle(detalle != null && detalle.length() > 500 ? detalle.substring(0, 500) : detalle);
        eventos.save(ev);
    }

    // ── Validaciones ────────────────────────────────────────────────────────

    String validarDni(String dni) {
        String d = dni == null ? "" : dni.trim();
        if (!d.matches("\\d{8}")) {
            throw new BotException("DNI_INVALIDO", "El DNI debe tener exactamente 8 dígitos numéricos.");
        }
        return d;
    }

    private Servicio servicioActivo(Long id) {
        return Optional.ofNullable(id).flatMap(servicios::findById).filter(s -> s.getEstado() == 1)
                .orElseThrow(() -> new BotException("SERVICIO_INVALIDO",
                        "Ese servicio no existe o no está disponible. Usa solo los del catálogo."));
    }

    private Barbero barberoActivo(Long id) {
        return Optional.ofNullable(id).flatMap(barberos::findById).filter(b -> b.getEstado() == 1)
                .orElseThrow(() -> new BotException("BARBERO_INVALIDO",
                        "Ese barbero no existe o no está atendiendo. Usa solo los del catálogo."));
    }

    private LocalDate validarFecha(String texto, Barbero barbero) {
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(texto == null ? "" : texto.trim());
        } catch (DateTimeParseException e) {
            throw new BotException("FECHA_INVALIDA", "La fecha debe ir en formato AAAA-MM-DD.");
        }
        LocalDate hoy = ahora().toLocalDate();
        if (fecha.isBefore(hoy)) {
            throw new BotException("FECHA_PASADA", "No se puede reservar en una fecha que ya pasó.",
                    Map.of("hoy", hoy.toString()));
        }
        if (fecha.isAfter(hoy.plusDays(diasAnticipacion))) {
            throw new BotException("FECHA_MUY_LEJANA",
                    "Solo se reserva con hasta " + diasAnticipacion + " días de anticipación.");
        }
        String dia = DIAS[fecha.getDayOfWeek().getValue() - 1];
        if (barbero.getDiaLibre() != null && dia.equalsIgnoreCase(barbero.getDiaLibre())) {
            throw new BotException("DIA_LIBRE_BARBERO",
                    barbero.getNombre() + " no atiende los " + dia.toLowerCase() + ". Ofrece otro día u otro barbero.");
        }
        return fecha;
    }

    private LocalTime validarHora(String texto, LocalDate fecha, int duracion) {
        LocalTime hora;
        try {
            hora = LocalTime.parse(texto == null ? "" : texto.trim());
        } catch (DateTimeParseException e) {
            throw new BotException("HORA_INVALIDA", "La hora debe ir en formato HH:mm de 24 horas, por ejemplo 15:30.");
        }
        if (hora.isBefore(apertura) || hora.plusMinutes(duracion).isAfter(cierre)
                || hora.getMinute() % intervaloMin != 0) {
            throw new BotException("FUERA_DE_HORARIO", "Atendemos de " + apertura + " a " + cierre
                    + " en turnos cada " + intervaloMin + " minutos.");
        }
        if (!LocalDateTime.of(fecha, hora).isAfter(ahora())) {
            throw new BotException("HORA_PASADA", "Esa hora de hoy ya pasó.");
        }
        return hora;
    }

    // ── Apoyo ───────────────────────────────────────────────────────────────

    private List<String> horasLibres(Long barberoId, LocalDate fecha, int duracion) {
        List<String> libres = new ArrayList<>();
        LocalDateTime limite = ahora().plusMinutes(15);
        for (LocalTime h = apertura; !h.plusMinutes(duracion).isAfter(cierre); h = h.plusMinutes(intervaloMin)) {
            if (LocalDateTime.of(fecha, h).isAfter(limite)
                    && !citas.existeConflictoHorario(barberoId, fecha, h, h.plusMinutes(duracion))) {
                libres.add(h.toString());
            }
        }
        return libres;
    }

    private List<String> alternativas(Long barberoId, LocalDate fecha, int duracion, LocalTime pedida) {
        List<String> libres = horasLibres(barberoId, fecha, duracion);
        libres.sort((a, b) -> Long.compare(
                Math.abs(java.time.Duration.between(pedida, LocalTime.parse(a)).toMinutes()),
                Math.abs(java.time.Duration.between(pedida, LocalTime.parse(b)).toMinutes())));
        return libres.subList(0, Math.min(3, libres.size()));
    }

    private List<Map<String, Object>> citasActivas(Cliente cliente) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Cita c : citas.findByClienteIdAndEstadoInOrderByFechaAscHoraInicioAsc(cliente.getId(), ESTADOS_ACTIVOS)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codigo", codigo(c));
            m.put("servicio", c.getServicio().getNombre());
            m.put("barbero", c.getBarbero().getNombre());
            m.put("fecha", c.getFecha().toString());
            m.put("hora", c.getHoraInicio().toString());
            lista.add(m);
        }
        return lista;
    }

    private Map<String, Object> comprobante(Cita c) {
        Map<String, Object> r = ok();
        r.put("codigo", codigo(c));
        r.put("cliente", c.getCliente().getNombres() + " " + c.getCliente().getApellidos());
        r.put("servicio", c.getServicio().getNombre());
        r.put("barbero", c.getBarbero().getNombre());
        r.put("fecha", c.getFecha().toString());
        r.put("diaSemana", DIAS[c.getFecha().getDayOfWeek().getValue() - 1]);
        r.put("hora", c.getHoraInicio().toString());
        r.put("precio", c.getTotalPrecio());
        r.put("estado", "PENDIENTE");
        r.put("indicaciones", "Pago en el local (Yape o efectivo). Llegar 5 minutos antes.");
        return r;
    }

    private static String codigo(Cita c) {
        return String.format("LC-%05d", c.getId());
    }

    /** Devuelve {nombres, apellidos} si la API de DNI responde. Si falla, el bot pide el nombre al cliente. */
    private Optional<String[]> consultarReniec(String dni) {
        try {
            JsonNode raiz = json.readTree(dniService.consultarDni(dni));
            JsonNode d = raiz.path("datos");
            if (raiz.path("success").asBoolean(false) && d.hasNonNull("nombres")) {
                String apellidos = (d.path("ape_paterno").asText("") + " " + d.path("ape_materno").asText("")).trim();
                return Optional.of(new String[] { d.path("nombres").asText(), apellidos });
            }
        } catch (Exception ignorado) {
            // Sin RENIEC el flujo sigue: se le pide el nombre al cliente
        }
        return Optional.empty();
    }

    private static String limpiar(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    private static String nvl(String s) {
        return s == null ? "-" : s;
    }

    private static Map<String, Object> ok() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        return r;
    }
}
