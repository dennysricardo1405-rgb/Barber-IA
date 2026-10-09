package com.example.BarberiaLaClasica.config;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.BarberiaLaClasica.model.Barbero;
import com.example.BarberiaLaClasica.model.Categoria;
import com.example.BarberiaLaClasica.model.Cita;
import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.model.CompraProveedor;
import com.example.BarberiaLaClasica.model.DetalleNotaVenta;
import com.example.BarberiaLaClasica.model.GastoLocal;
import com.example.BarberiaLaClasica.model.HistorialInventario;
import com.example.BarberiaLaClasica.model.NotaVenta;
import com.example.BarberiaLaClasica.model.Producto;
import com.example.BarberiaLaClasica.model.Promocion;
import com.example.BarberiaLaClasica.model.Proveedor;
import com.example.BarberiaLaClasica.model.Servicio;
import com.example.BarberiaLaClasica.model.SillaSession;
import com.example.BarberiaLaClasica.model.Usuario;
import com.example.BarberiaLaClasica.repository.BarberoRepository;
import com.example.BarberiaLaClasica.repository.CategoriaRepository;
import com.example.BarberiaLaClasica.repository.CitaRepository;
import com.example.BarberiaLaClasica.repository.ClienteRepository;
import com.example.BarberiaLaClasica.repository.CompraProveedorRepository;
import com.example.BarberiaLaClasica.repository.GastoLocalRepository;
import com.example.BarberiaLaClasica.repository.HistorialInventarioRepository;
import com.example.BarberiaLaClasica.repository.NotaVentaRepository;
import com.example.BarberiaLaClasica.repository.PerfilRepository;
import com.example.BarberiaLaClasica.repository.ProductoRepository;
import com.example.BarberiaLaClasica.repository.PromocionRepository;
import com.example.BarberiaLaClasica.repository.ProveedorRepository;
import com.example.BarberiaLaClasica.repository.ServicioRepository;
import com.example.BarberiaLaClasica.repository.SillaSessionRepository;
import com.example.BarberiaLaClasica.repository.UsuarioRepository;

/**
 * Datos de demostración ficticios pero coherentes (barberos, servicios, productos, clientes
 * y 90 días de historial de atenciones). Solo se ejecuta con DEMO_DATA=true y cuando la base
 * no tiene barberos, así que nunca toca una base con datos reales.
 *
 * Cada cliente tiene su propio ciclo de visitas (cada 2 a 5 semanas) y algunos dejaron de venir:
 * sirve para probar reportes, sueldos, el kardex y, más adelante, el recordatorio predictivo (U4).
 */
@Component
@Order(2)
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final int DIAS_HISTORIAL = 90;

    private final boolean habilitado;
    private final BarberoRepository barberoRepository;
    private final ServicioRepository servicioRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final CompraProveedorRepository compraRepository;
    private final HistorialInventarioRepository historialRepository;
    private final ClienteRepository clienteRepository;
    private final CitaRepository citaRepository;
    private final SillaSessionRepository sesionRepository;
    private final NotaVentaRepository notaRepository;
    private final GastoLocalRepository gastoRepository;
    private final PromocionRepository promocionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    private final Random random = new Random(2026);

    public DemoDataInitializer(@Value("${app.demo-data:false}") boolean habilitado,
            BarberoRepository barberoRepository, ServicioRepository servicioRepository,
            CategoriaRepository categoriaRepository, ProductoRepository productoRepository,
            ProveedorRepository proveedorRepository, CompraProveedorRepository compraRepository,
            HistorialInventarioRepository historialRepository, ClienteRepository clienteRepository,
            CitaRepository citaRepository, SillaSessionRepository sesionRepository,
            NotaVentaRepository notaRepository, GastoLocalRepository gastoRepository,
            PromocionRepository promocionRepository, UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository, BCryptPasswordEncoder passwordEncoder, JdbcTemplate jdbc) {
        this.habilitado = habilitado;
        this.barberoRepository = barberoRepository;
        this.servicioRepository = servicioRepository;
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
        this.compraRepository = compraRepository;
        this.historialRepository = historialRepository;
        this.clienteRepository = clienteRepository;
        this.citaRepository = citaRepository;
        this.sesionRepository = sesionRepository;
        this.notaRepository = notaRepository;
        this.gastoRepository = gastoRepository;
        this.promocionRepository = promocionRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!habilitado) {
            return;
        }
        if (barberoRepository.count() > 0) {
            log.info("DEMO_DATA activo, pero la base ya tiene barberos: no se cargan datos de demostración.");
            return;
        }
        log.info("Cargando datos de demostración...");

        crearSecretario();
        List<Barbero> barberos = crearBarberos();
        List<Servicio> servicios = crearServicios();
        List<Producto> productos = crearProductosConStock();
        List<Cliente> clientes = crearClientes();
        int atenciones = crearHistorial(barberos, servicios, productos, clientes);
        crearReservasDeHoy(barberos, servicios, clientes);
        crearGastos();
        crearPromocion(servicios);

        log.info("Datos de demostración listos: {} barberos, {} clientes, {} atenciones en {} días.",
                barberos.size(), clientes.size(), atenciones, DIAS_HISTORIAL);
    }

    // ── Personal ─────────────────────────────────────────────────────────────

    private void crearSecretario() {
        if (usuarioRepository.findByEmail("secretario@gmail.com").isPresent()) {
            return;
        }
        perfilRepository.findByNombrePerfil("Secretario").ifPresent(perfil -> {
            Usuario u = new Usuario();
            u.setNombre("Rosa Huamán");
            u.setEmail("secretario@gmail.com");
            u.setPassword(passwordEncoder.encode("secretario123"));
            u.setEstado(1);
            u.setPerfil(perfil);
            usuarioRepository.save(u);
        });
    }

    private List<Barbero> crearBarberos() {
        String[][] datos = {
                { "Carlos Ramírez", "Degradados y fades", "987654321", "LUNES" },
                { "Luis Torres", "Cortes clásicos", "976543210", "MARTES" },
                { "Miguel Paredes", "Barba y perfilado", "965432109", "MIERCOLES" },
                { "Jorge Salazar", "Diseños y cortes modernos", "954321098", "JUEVES" },
        };
        List<Barbero> barberos = new ArrayList<>();
        for (String[] d : datos) {
            Barbero b = new Barbero();
            b.setNombre(d[0]);
            b.setEspecialidad(d[1]);
            b.setTelefono(d[2]);
            b.setDiaLibre(d[3]);
            barberos.add(barberoRepository.save(b));
        }
        return barberos;
    }

    private List<Servicio> crearServicios() {
        Object[][] datos = {
                { "Corte clásico", "Corte a tijera y máquina con acabado tradicional", 20, 30 },
                { "Corte + degradado", "Fade bajo, medio o alto con volumen arriba", 25, 40 },
                { "Perfilado de barba", "Perfilado con navaja y toalla caliente", 15, 20 },
                { "Corte + barba", "Corte completo más perfilado de barba", 35, 50 },
                { "Corte niño", "Corte para niños hasta 12 años", 15, 25 },
                { "Diseño / líneas", "Diseño con navaja sobre el corte", 10, 15 },
        };
        List<Servicio> servicios = new ArrayList<>();
        for (Object[] d : datos) {
            Servicio s = new Servicio();
            s.setNombre((String) d[0]);
            s.setDescripcion((String) d[1]);
            s.setPrecio(BigDecimal.valueOf((Integer) d[2]));
            s.setDuracionMinutos((Integer) d[3]);
            servicios.add(servicioRepository.save(s));
        }
        return servicios;
    }

    // ── Inventario ───────────────────────────────────────────────────────────

    private List<Producto> crearProductosConStock() {
        Categoria cabello = categoria("Cuidado del cabello", "Productos para peinar y lavar", null);
        Categoria ceras = categoria("Ceras y pomadas", "Fijación y acabado", cabello);
        Categoria shampoo = categoria("Shampoo y acondicionador", "Limpieza del cabello", cabello);
        Categoria barba = categoria("Cuidado de la barba", "Aceites y bálsamos", null);
        Categoria bebidas = categoria("Bebidas", "Bebidas para los clientes", null);

        Proveedor distribuidora = proveedor("Distribuidora Norte Barber S.A.C.", "074231456", "20601234567");
        Proveedor bebidasProv = proveedor("Bebidas Lambayeque E.I.R.L.", "074245789", "20487654321");
        proveedor("Importaciones Estilo Perú S.A.C.", "014567890", "20554433221");

        Object[][] datos = {
                { "Cera mate Reuzel 113 g", "Fijación fuerte, acabado mate", 45.0, ceras, distribuidora, 30.0 },
                { "Pomada brillo Suavecito 113 g", "Fijación media con brillo", 42.0, ceras, distribuidora, 28.0 },
                { "Polvo texturizador 20 g", "Volumen y textura", 35.0, ceras, distribuidora, 22.0 },
                { "Shampoo anticaspa 400 ml", "Uso diario", 28.0, shampoo, distribuidora, 17.0 },
                { "Aceite para barba 30 ml", "Hidrata y suaviza", 38.0, barba, distribuidora, 24.0 },
                { "Bálsamo para barba 60 g", "Peina y controla", 32.0, barba, distribuidora, 20.0 },
                { "Agua mineral 625 ml", "Sin gas", 2.5, bebidas, bebidasProv, 1.2 },
                { "Gaseosa 500 ml", "Personal", 3.5, bebidas, bebidasProv, 2.0 },
        };
        List<Producto> productos = new ArrayList<>();
        LocalDateTime fechaCompra = LocalDate.now().minusDays(DIAS_HISTORIAL + 1).atTime(10, 0);
        for (Object[] d : datos) {
            Producto p = new Producto();
            p.setNombre((String) d[0]);
            p.setDescripcion((String) d[1]);
            p.setPrecioVenta((Double) d[2]);
            p.setCategoria((Categoria) d[3]);
            boolean esBebida = d[3] == bebidas;
            int unidades = esBebida ? 120 : 40;
            p.setStock(unidades);
            p = productoRepository.save(p);
            productos.add(p);

            CompraProveedor compra = new CompraProveedor();
            compra.setProveedor((Proveedor) d[4]);
            compra.setProducto(p);
            compra.setTipoCompra(CompraProveedor.TipoCompra.PAQUETE);
            compra.setUnidadesPorPaquete(esBebida ? 12 : 10);
            compra.setCantidadPaquetes(unidades / (esBebida ? 12 : 10));
            compra.setTotalUnidades(unidades);
            compra.setPrecioCompraPaquete((Double) d[5] * (esBebida ? 12 : 10));
            compra.setPrecioVentaUnidad((Double) d[2]);
            compra.setTotalInvertido((Double) d[5] * unidades);
            compra = compraRepository.save(compra);
            jdbc.update("UPDATE compras_proveedor SET fecha_compra = ? WHERE id = ?", fechaCompra, compra.getId());

            HistorialInventario entrada = new HistorialInventario();
            entrada.setProducto(p);
            entrada.setTipoMovimiento("ENTRADA");
            entrada.setCantidad(unidades);
            entrada.setStockResultante(unidades);
            entrada.setMotivo("Compra a proveedor: " + ((Proveedor) d[4]).getNombre());
            entrada.setFecha(fechaCompra);
            historialRepository.save(entrada);
        }
        return productos;
    }

    private Categoria categoria(String nombre, String descripcion, Categoria padre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setDescripcion(descripcion);
        c.setPadre(padre);
        return categoriaRepository.save(c);
    }

    private Proveedor proveedor(String nombre, String telefono, String ruc) {
        Proveedor p = new Proveedor();
        p.setNombre(nombre);
        p.setTelefono(telefono);
        p.setRuc(ruc);
        return proveedorRepository.save(p);
    }

    // ── Clientes ─────────────────────────────────────────────────────────────

    private List<Cliente> crearClientes() {
        String[][] nombres = {
                { "Juan", "Pérez Quispe" }, { "Diego", "Sánchez Vera" }, { "Renato", "Castillo Ruiz" },
                { "Kevin", "Flores Mendoza" }, { "Bruno", "Chávez Díaz" }, { "Alonso", "Gutiérrez Silva" },
                { "Sebastián", "Rojas Campos" }, { "Matías", "Vásquez León" }, { "Andrés", "Torres Llontop" },
                { "Fabricio", "Acosta Bances" }, { "Rodrigo", "Ñique Santamaría" }, { "Gabriel", "Delgado Puican" },
                { "Joaquín", "Cabrera Chapoñán" }, { "Luis", "Montenegro Díaz" }, { "César", "Effio Carrasco" },
                { "Piero", "Bravo Ugaz" }, { "Marco", "Lozada Fernández" }, { "Iván", "Seclén Tantaleán" },
                { "Hugo", "Neciosup Alvarado" }, { "Daniel", "Zapata Ríos" },
        };
        String clave = passwordEncoder.encode("cliente123");
        List<Cliente> clientes = new ArrayList<>();
        for (int i = 0; i < nombres.length; i++) {
            Cliente c = new Cliente();
            c.setDni(String.valueOf(70000001 + i * 3137));
            c.setNombres(nombres[i][0]);
            c.setApellidos(nombres[i][1]);
            c.setTelefono("9" + (10000000 + random.nextInt(89999999)));
            c.setCorreo(sinTildes(nombres[i][0] + "." + nombres[i][1].split(" ")[0]).toLowerCase() + "@gmail.com");
            c.setPassword(clave);
            clientes.add(clienteRepository.save(c));
        }
        jdbc.update("UPDATE clientes SET fecha_registro = ? WHERE dni LIKE '7%'",
                LocalDate.now().minusDays(DIAS_HISTORIAL + 5).atTime(9, 0));
        return clientes;
    }

    private static String sinTildes(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace("ñ", "n").replace("Ñ", "N");
    }

    // ── Historial de atenciones ──────────────────────────────────────────────

    private int crearHistorial(List<Barbero> barberos, List<Servicio> servicios, List<Producto> productos,
            List<Cliente> clientes) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy.minusDays(DIAS_HISTORIAL);
        Map<Producto, Integer> stock = new HashMap<>();
        productos.forEach(p -> stock.put(p, p.getStock()));
        int atenciones = 0;

        // Clientes registrados: cada uno con su ciclo, barbero y servicio habituales
        for (int i = 0; i < clientes.size(); i++) {
            Cliente cliente = clientes.get(i);
            int ciclo = 14 + random.nextInt(22);
            Barbero barbero = barberos.get(random.nextInt(barberos.size()));
            Servicio servicio = servicios.get(random.nextInt(4));
            // 1 de cada 4 clientes dejó de venir: su última visita fue hace más de un ciclo
            LocalDate fin = i % 4 == 3 ? hoy.minusDays(ciclo + 10 + random.nextInt(15)) : hoy.minusDays(1);
            LocalDate fecha = inicio.plusDays(random.nextInt(ciclo));
            while (!fecha.isAfter(fin)) {
                LocalDate dia = moverSiEsDiaLibre(fecha, barbero);
                if (!dia.isAfter(fin)) {
                    boolean reservaWeb = random.nextInt(100) < 45;
                    atender(dia, barbero, servicio, cliente, reservaWeb, productos, stock);
                    atenciones++;
                }
                fecha = fecha.plusDays(ciclo + random.nextInt(7) - 3);
            }
        }

        // Clientes de paso (sin registro), 2 a 5 por día
        for (LocalDate dia = inicio; dia.isBefore(hoy); dia = dia.plusDays(1)) {
            if (dia.getDayOfWeek() == DayOfWeek.SUNDAY && random.nextBoolean()) {
                continue;
            }
            int dePaso = 2 + random.nextInt(4);
            for (int k = 0; k < dePaso; k++) {
                Barbero barbero = barberos.get(random.nextInt(barberos.size()));
                if (esDiaLibre(dia, barbero)) {
                    continue;
                }
                atender(dia, barbero, servicios.get(random.nextInt(servicios.size())), null, false, productos, stock);
                atenciones++;
            }
        }

        stock.forEach((p, s) -> {
            p.setStock(s);
            productoRepository.save(p);
        });
        return atenciones;
    }

    private void atender(LocalDate dia, Barbero barbero, Servicio servicio, Cliente cliente, boolean reservaWeb,
            List<Producto> productos, Map<Producto, Integer> stock) {
        LocalTime hora = LocalTime.of(9 + random.nextInt(11), random.nextBoolean() ? 0 : 30);
        LocalDateTime momento = dia.atTime(hora);
        BigDecimal precio = servicio.getPrecio();

        Cita cita = null;
        if (reservaWeb && cliente != null) {
            cita = new Cita();
            cita.setCliente(cliente);
            cita.setBarbero(barbero);
            cita.setServicio(servicio);
            cita.setFecha(dia);
            cita.setHoraInicio(hora);
            cita.setHoraFin(hora.plusMinutes(servicio.getDuracionMinutos()));
            cita.setTotalPrecio(precio);
            cita.setMontoYape(precio);
            cita.setCodigoYape(String.valueOf(100000 + random.nextInt(899999)));
            cita.setEstado(3);
            cita = citaRepository.save(cita);
            jdbc.update("UPDATE citas SET fecha_registro = ? WHERE id = ?", momento.minusDays(1 + random.nextInt(4)),
                    cita.getId());
        }

        SillaSession sesion = new SillaSession();
        sesion.setBarbero(barbero);
        sesion.setCliente(cliente);
        sesion.setServicio(servicio);
        sesion.setCita(cita);
        sesion.setEstado(0);
        sesion = sesionRepository.save(sesion);
        jdbc.update("UPDATE silla_sessions SET inicio = ? WHERE id = ?", momento, sesion.getId());

        NotaVenta nota = new NotaVenta();
        nota.setSession(sesion);
        nota.setBarbero(barbero);
        nota.setCliente(cliente);
        List<DetalleNotaVenta> detalles = new ArrayList<>();
        detalles.add(detalle(nota, servicio.getNombre(), 1, precio.doubleValue(), "SERVICIO"));

        // 1 de cada 4 atenciones incluye un producto (más bebidas que ceras)
        if (random.nextInt(4) == 0) {
            Producto p = productos.get(random.nextInt(10) < 6 ? 6 + random.nextInt(2) : random.nextInt(6));
            if (stock.get(p) > 0) {
                stock.put(p, stock.get(p) - 1);
                detalles.add(detalle(nota, p.getNombre(), 1, p.getPrecioVenta(), "PRODUCTO"));
                HistorialInventario salida = new HistorialInventario();
                salida.setProducto(p);
                salida.setTipoMovimiento("SALIDA");
                salida.setCantidad(1);
                salida.setStockResultante(stock.get(p));
                salida.setMotivo("Venta en Caja - Atendido por Barbero: " + barbero.getNombre());
                salida.setFecha(momento.plusMinutes(servicio.getDuracionMinutos()));
                historialRepository.save(salida);
            }
        }

        double total = detalles.stream().mapToDouble(DetalleNotaVenta::getSubtotal).sum();
        nota.setDetalles(detalles);
        nota.setTotal(total);
        if (cita != null) {
            nota.setMetodoPago("YAPE");
            nota.setMontoYape(precio.doubleValue());
            nota.setMontoEfectivo(total - precio.doubleValue());
            nota.setCodigoYape(cita.getCodigoYape());
        } else if (random.nextInt(100) < 40) {
            nota.setMetodoPago("YAPE");
            nota.setMontoYape(total);
            nota.setCodigoYape(String.valueOf(100000 + random.nextInt(899999)));
        } else {
            nota.setMetodoPago("EFECTIVO");
            nota.setMontoEfectivo(total);
        }
        nota = notaRepository.save(nota);
        jdbc.update("UPDATE notas_venta SET fecha = ? WHERE id = ?",
                momento.plusMinutes(servicio.getDuracionMinutos()), nota.getId());
    }

    private static DetalleNotaVenta detalle(NotaVenta nota, String descripcion, int cantidad, double precio,
            String tipo) {
        DetalleNotaVenta d = new DetalleNotaVenta();
        d.setNotaVenta(nota);
        d.setDescripcion(descripcion);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(precio);
        d.setSubtotal(precio * cantidad);
        d.setTipo(tipo);
        return d;
    }

    private LocalDate moverSiEsDiaLibre(LocalDate fecha, Barbero barbero) {
        return esDiaLibre(fecha, barbero) ? fecha.plusDays(1) : fecha;
    }

    private static boolean esDiaLibre(LocalDate fecha, Barbero barbero) {
        String dia = sinTildes(fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "PE")))
                .toUpperCase();
        return dia.equals(barbero.getDiaLibre());
    }

    // ── Agenda de hoy, gastos y promoción ────────────────────────────────────

    private void crearReservasDeHoy(List<Barbero> barberos, List<Servicio> servicios, List<Cliente> clientes) {
        LocalDate hoy = LocalDate.now();
        int[][] agenda = { { 0, 16, 0 }, { 1, 17, 1 }, { 2, 18, 3 } }; // barbero, hora, servicio
        for (int i = 0; i < agenda.length; i++) {
            Servicio s = servicios.get(agenda[i][2]);
            Cita c = new Cita();
            c.setCliente(clientes.get(i * 5));
            c.setBarbero(barberos.get(agenda[i][0]));
            c.setServicio(s);
            c.setFecha(hoy);
            c.setHoraInicio(LocalTime.of(agenda[i][1], 0));
            c.setHoraFin(LocalTime.of(agenda[i][1], 0).plusMinutes(s.getDuracionMinutos()));
            c.setTotalPrecio(s.getPrecio());
            c.setMontoYape(s.getPrecio());
            c.setCodigoYape(String.valueOf(100000 + random.nextInt(899999)));
            c.setEstado(2); // confirmada: aparece en Recepción como "Con Reserva"
            citaRepository.save(c);
        }
        // Una reserva pendiente de confirmar para mañana
        Servicio s = servicios.get(1);
        Cita pendiente = new Cita();
        pendiente.setCliente(clientes.get(2));
        pendiente.setBarbero(barberos.get(3));
        pendiente.setServicio(s);
        pendiente.setFecha(hoy.plusDays(1));
        pendiente.setHoraInicio(LocalTime.of(11, 0));
        pendiente.setHoraFin(LocalTime.of(11, 0).plusMinutes(s.getDuracionMinutos()));
        pendiente.setTotalPrecio(s.getPrecio());
        pendiente.setEstado(1);
        citaRepository.save(pendiente);
    }

    private void crearGastos() {
        LocalDate hoy = LocalDate.now();
        for (int m = 0; m < 3; m++) {
            LocalDate mes = hoy.minusMonths(m).withDayOfMonth(1);
            gasto("Alquiler del local", 1200, mes.atTime(9, 0));
            gasto("Luz y agua", 180 + random.nextInt(60), mes.plusDays(9).atTime(12, 0));
            gasto("Internet", 99, mes.plusDays(14).atTime(12, 0));
            gasto("Toallas, navajas y desinfectante", 70 + random.nextInt(50), mes.plusDays(4).atTime(11, 0));
        }
    }

    private void gasto(String descripcion, double monto, LocalDateTime fecha) {
        if (fecha.isAfter(LocalDateTime.now())) {
            return;
        }
        GastoLocal g = new GastoLocal();
        g.setDescripcion(descripcion);
        g.setMonto(monto);
        g.setFecha(fecha);
        gastoRepository.save(g);
    }

    private void crearPromocion(List<Servicio> servicios) {
        Promocion p = new Promocion();
        p.setNombre("Cliente frecuente: Corte + barba");
        p.setDescripcion("10% de descuento desde la 5ta visita");
        p.setTipoPromocion("SERVICIO");
        p.setServicio(servicios.get(3));
        p.setPorcentajeDescuento(BigDecimal.TEN);
        p.setMinimoVisitasRequeridas(5);
        p.setFechaInicio(LocalDate.now().minusDays(15).atStartOfDay());
        p.setFechaFin(LocalDate.now().plusDays(45).atTime(23, 59));
        promocionRepository.save(p);
    }
}
