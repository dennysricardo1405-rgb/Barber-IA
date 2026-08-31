package com.example.BarberiaLaClasica.service;

import com.example.BarberiaLaClasica.model.*;
import com.example.BarberiaLaClasica.repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class CitaService {

    @Autowired
    private CitaRepository citaRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private BarberoRepository barberoRepository;
    @Autowired
    private ServicioRepository servicioRepository;
    @Autowired
    private PromocionHelper promocionHelper;
    @Autowired
    private ClienteService clienteService;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mailFrom;

    // Carpeta donde se guardan los comprobantes (configura en
    // application.properties)
    @Value("${app.upload.dir:uploads/comprobantes}")
    private String uploadDir;

    // ── Historial de citas de un cliente ──────────────────────────────────────
    public List<Cita> obtenerHistorialCliente(String correo) {
        Cliente cliente = clienteRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + correo));
        return citaRepository.findByClienteOrderByFechaDescHoraInicioDesc(cliente);
    }

    // ── Horas disponibles para un barbero en una fecha ───────────────────────

    // ── Confirmar reserva: guarda la cita con comprobante de pago ─────────────
    @Transactional
    public Cita confirmarReserva(
            String correoCliente,
            Long servicioId,
            Long barberoId,
            String fechaStr,
            String horaStr,
            MultipartFile comprobante) throws IOException {

        Cliente cliente = clienteRepository.findByCorreo(correoCliente)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
        Barbero barbero = barberoRepository.findById(barberoId)
                .orElseThrow(() -> new RuntimeException("Barbero no encontrado"));

        LocalDate fecha = LocalDate.parse(fechaStr);
        LocalTime horaInicio = LocalTime.parse(horaStr);
        LocalTime horaFin = horaInicio.plusMinutes(servicio.getDuracionMinutos());

        long reservasActivas = citaRepository.contarReservasActivasPorCliente(cliente.getId());
        if (reservasActivas >= 1) {
            throw new RuntimeException("Ya tienes una reserva pendiente o confirmada. Espera a ser atendido.");
        }

        if (citaRepository.existeConflictoHorario(barberoId, fecha, horaInicio, horaFin)) {
            throw new RuntimeException("El horario seleccionado ya no está disponible.");
        }

        // Guardar la captura en el disco
        String nombreArchivo = UUID.randomUUID() + "_" + comprobante.getOriginalFilename();
        Path ruta = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(ruta);
        Files.copy(comprobante.getInputStream(), ruta.resolve(nombreArchivo), StandardCopyOption.REPLACE_EXISTING);

        Cita cita = new Cita();
        cita.setCliente(cliente);
        cita.setBarbero(barbero);
        cita.setServicio(servicio);
        cita.setFecha(fecha);
        cita.setHoraInicio(horaInicio);
        cita.setHoraFin(horaFin);
        if (cliente != null) {
            cliente.setTotalVisitas(clienteService.calcularTotalVisitas(cliente));
        }
        double precioFinalServicio = promocionHelper.calcularPrecioServicio(servicio, cliente);
        cita.setTotalPrecio(BigDecimal.valueOf(precioFinalServicio));
        cita.setComprobantePago(nombreArchivo);

        // Se inicializan en blanco, el secretario rellenará los campos reales usando el modal interactivo
        cita.setMontoYape(BigDecimal.ZERO);
        cita.setMontoEfectivo(BigDecimal.ZERO);
        cita.setCodigoYape("PROCESANDO_OCR");
        cita.setEstado(1); // PENDIENTE

        return citaRepository.save(cita);
    }

    // ── Secretario: cambiar estado a ACEPTADO (2) y notificar por WhatsApp ────
    @Transactional
    public void aceptarCitaHibridaCompleta(Long citaId, BigDecimal yapeReal, BigDecimal efectivoReal, String codigoReal) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        cita.setMontoYape(yapeReal);
        cita.setMontoEfectivo(efectivoReal);
        
        if (codigoReal != null && !codigoReal.equals("NO_DETECTADO") && !codigoReal.equals("Escaneando...")) {
            cita.setCodigoYape(codigoReal);
        }

        cita.setEstado(2); // CONFIRMADA / EN SILLA
        citaRepository.save(cita);

        if (cita.getCliente() != null && cita.getCliente().getCorreo() != null) {
            enviarEmail(cita);
        }
    }

    // ── Secretario: cancelar cita ─────────────────────────────────────────────
    @Transactional
    public void cancelarCita(Long citaId) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        cita.setEstado(0); // CANCELADA
        citaRepository.save(cita);
    }

    private void enviarEmail(Cita cita) {
    try {
        // ── PARCHE DE PRUEBAS PARA RAILWAY ──
        // Interrumpimos el flujo de inmediato para evitar bloqueos por SMTP
        System.out.println("Envío de correo omitido para el cliente: " + cita.getCliente().getCorreo());
        return; 

        /* * El código original se queda empaquetado y comentado automáticamente debajo.
         * Cuando configures tu dominio SMTP real en el futuro, solo borras las líneas del parche superior.
         *
        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm");

        String asunto = "Tu cita en Barbería La Clásica fue confirmada";

        String cuerpo = """
                <div style="font-family:Arial,sans-serif; max-width:520px; margin:0 auto; background:#111; color:#f0ece0; border-radius:12px; overflow:hidden;">
                    <div style="background:#c9a84c; padding:24px; text-align:center;">
                        <h2 style="margin:0; color:#0a0a0a;">Barbería La Clásica</h2>
                        <p style="margin:4px 0 0; color:#0a0a0a; font-size:0.9rem;">Confirmación de Cita</p>
                    </div>
                    <div style="padding:28px;">
                        <p style="font-size:1rem;">Hola <strong>%s</strong>, tu cita ha sido <strong style="color:#c9a84c;">CONFIRMADA</strong> 🎉</p>
                        <table style="width:100%%; border-collapse:collapse; margin-top:16px;">
                            <tr><td style="padding:10px; border-bottom:1px solid #222; color:#aaa;">Fecha</td>
                                <td style="padding:10px; border-bottom:1px solid #222; font-weight:bold;">%s</td></tr>
                            <tr><td style="padding:10px; border-bottom:1px solid #222; color:#aaa;">Hora</td>
                                <td style="padding:10px; border-bottom:1px solid #222; font-weight:bold;">%s</td></tr>
                            <tr><td style="padding:10px; border-bottom:1px solid #222; color:#aaa;">Servicio</td>
                                <td style="padding:10px; border-bottom:1px solid #222; font-weight:bold;">%s</td></tr>
                            <tr><td style="padding:10px; color:#aaa;">Barbero</td>
                                <td style="padding:10px; font-weight:bold;">%s</td></tr>
                        </table>
                        <p style="margin-top:24px; font-size:0.85rem; color:#aaa;">Por favor llega 5 minutos antes. ¡Te esperamos!</p>
                    </div>
                    <div style="background:#1a1a1a; padding:14px; text-align:center; font-size:0.78rem; color:#555;">
                        Barbería La Clásica — Chiclayo, Perú
                    </div>
                </div>
                """
                .formatted(
                        cita.getCliente().getNombres(),
                        cita.getFecha().format(fmtFecha),
                        cita.getHoraInicio().format(fmtHora),
                        cita.getServicio().getNombre(),
                        cita.getBarbero().getNombre());

        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
        helper.setFrom(mailFrom);
        helper.setTo(cita.getCliente().getCorreo());
        helper.setSubject(asunto);
        helper.setText(cuerpo, true);

        mailSender.send(mensaje);
        System.out.println("✅ Email enviado a: " + cita.getCliente().getCorreo());
        */

    } catch (Exception e) {
        System.err.println("ERROR CORREO COMPLETO: " + e.getClass().getName()
                + " — " + e.getMessage());
        if (e.getCause() != null)
            System.err.println("CAUSA: " + e.getCause().getMessage());
    }
}

    // Historial paginado
    public Page<Cita> obtenerHistorialClientePaginado(String correo, int pagina) {
        Cliente cliente = clienteRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        Pageable pageable = PageRequest.of(pagina, 5);
        return citaRepository.findByClienteOrderByFechaDescHoraInicioDesc(cliente, pageable);
    }

    // Cancelar cita propia del cliente
    public void cancelarCitaCliente(Long citaId, String correoCliente) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        Cliente cliente = clienteRepository.findByCorreo(correoCliente)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (!cita.getCliente().getId().equals(cliente.getId()))
            throw new RuntimeException("No tienes permiso para cancelar esta cita");

        if (cita.getEstado() != 1 && cita.getEstado() != 2)
            throw new RuntimeException("Esta cita no puede cancelarse");

        cita.setEstado(0);
        citaRepository.save(cita);
    }

    // Reprogramar cita (solo fecha y hora, mismo barbero o nuevo)
    public void reprogramarCita(Long citaId, String correoCliente,
            Long nuevoBarberoId, String nuevaFecha,
            String nuevaHora) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        Cliente cliente = clienteRepository.findByCorreo(correoCliente)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        if (cita.isReprogramada())
            throw new RuntimeException(
                    "Esta cita ya fue reprogramada una vez. No se puede volver a cambiar.");
        if (!cita.getCliente().getId().equals(cliente.getId()))
            throw new RuntimeException("No tienes permiso");

        if (cita.getEstado() != 1 && cita.getEstado() != 2)
            throw new RuntimeException("Esta cita no puede reprogramarse");

        Barbero barbero = barberoRepository.findById(nuevoBarberoId)
                .orElseThrow(() -> new RuntimeException("Barbero no encontrado"));

        LocalDate fecha = LocalDate.parse(nuevaFecha);
        LocalTime horaInicio = LocalTime.parse(nuevaHora);
        LocalTime horaFin = horaInicio.plusMinutes(cita.getServicio().getDuracionMinutos());

        if (citaRepository.existeConflictoHorario(nuevoBarberoId, fecha, horaInicio, horaFin))
            throw new RuntimeException("Ese horario ya no está disponible");

        cita.setBarbero(barbero);
        cita.setFecha(fecha);
        cita.setHoraInicio(horaInicio);
        cita.setHoraFin(horaFin);
        cita.setEstado(1); // vuelve a pendiente para re-confirmación
        citaRepository.save(cita);
        cita.setReprogramada(true);
        citaRepository.save(cita);
    }

    // ── Para el secretario: listar citas pendientes ───────────────────────────
    public List<Cita> listarPendientes() {
        return citaRepository.findByEstadoOrderByFechaAscHoraInicioAsc(1);
    }

    public List<Cita> listarCitasDeHoy() {
        return citaRepository.findByFechaOrderByHoraInicioAsc(LocalDate.now());
    }

}