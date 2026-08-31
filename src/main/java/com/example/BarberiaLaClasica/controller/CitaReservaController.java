package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.service.CitaService;
import com.example.BarberiaLaClasica.service.PromocionHelper;
import com.example.BarberiaLaClasica.service.ConfiguracionSitioService;
import com.example.BarberiaLaClasica.repository.ServicioRepository;
import com.example.BarberiaLaClasica.model.Cita;
import com.example.BarberiaLaClasica.repository.BarberoRepository;
import com.example.BarberiaLaClasica.repository.CitaRepository;
import com.example.BarberiaLaClasica.repository.ClienteRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Controller
public class CitaReservaController {

    @Autowired
    private ServicioRepository servicioRepository;
    @Autowired
    private BarberoRepository barberoRepository;
    @Autowired
    private CitaService citaService;
    @Autowired
    private CitaRepository citaRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private com.example.BarberiaLaClasica.service.ClienteService clienteService;
    @Autowired
    private PromocionHelper promocionHelper;
    @Autowired
    private ConfiguracionSitioService configuracionSitioService;

    // ─────────────────────────────────────────────────────────────────
    // PASO 1-3: Asistente público (sin login requerido)
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/reservar")
    public String verAsistenteReserva(Model model, Principal principal) {
        com.example.BarberiaLaClasica.model.Cliente clienteLogueado = null;
        if (principal != null) {
            clienteLogueado = clienteRepository.findByCorreo(principal.getName()).orElse(null);
            if (clienteLogueado != null) {
                clienteLogueado.setTotalVisitas(clienteService.calcularTotalVisitas(clienteLogueado));
            }
        }

        model.addAttribute("clienteLogueado", clienteLogueado);
        model.addAttribute("servicios", servicioRepository.findByEstado(1));
        model.addAttribute("barberos", barberoRepository.findByEstado(1));
        model.addAttribute("promoHelper", promocionHelper);

        return "reserva/reservar-pasos";
    }

    @GetMapping("/api/citas/horas-disponibles")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> horasDisponibles(
            @RequestParam Long barberoId,
            @RequestParam String fecha) {

        LocalDate fechaDate = LocalDate.parse(fecha);

        List<Object[]> filas = citaRepository
                .findHorasConEstadoPorBarberoYFecha(barberoId, fechaDate);

        Map<LocalTime, Integer> horaEstado = new HashMap<>();
        for (Object[] fila : filas) {
            LocalTime hora = (LocalTime) fila[0];
            Integer estado = ((Number) fila[1]).intValue();
            horaEstado.merge(hora, estado, (existente, nuevo) -> nuevo == 2 ? nuevo : existente);
        }

        List<Map<String, Object>> slots = new ArrayList<>();
        LocalTime hora = LocalTime.of(10, 0);
        LocalTime cierre = LocalTime.of(22, 00);

        while (hora.isBefore(cierre)) {
            Map<String, Object> slot = new HashMap<>();
            slot.put("hora", hora.toString());

            if (!horaEstado.containsKey(hora)) {
                slot.put("disponible", true);
                slot.put("estadoOcupacion", null);
            } else {
                slot.put("disponible", false);
                slot.put("estadoOcupacion", horaEstado.get(hora) == 1 ? "pendiente" : "confirmada");
            }

            slots.add(slot);
            hora = hora.plusMinutes(30);
        }

        return ResponseEntity.ok(Map.of("slots", slots));
    }

    // API: Guarda la pre-reserva en sesión y verifica si requiere login
    @PostMapping("/api/citas/pre-reserva")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> preReserva(
            @RequestBody Map<String, Object> datos,
            HttpSession session,
            Principal principal) {

        session.setAttribute("preCita_servicioId", datos.get("servicioId"));
        session.setAttribute("preCita_barberoId", datos.get("barberoId"));
        session.setAttribute("preCita_fecha", datos.get("fecha"));
        session.setAttribute("preCita_hora", datos.get("hora"));

        Map<String, Object> resp = new HashMap<>();
        if (principal == null) {
            resp.put("requiereAutenticacion", true);
        } else {
            resp.put("requiereAutenticacion", false);
            resp.put("redireccion", "/cliente/reserva/pago");
        }
        return ResponseEntity.ok(resp);
    }

    // ─────────────────────────────────────────────────────────────────
    // PASO 4: Pago con Yape (requiere login)
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/cliente/reserva/pago")
    public String pantallaPago(HttpSession session, Model model, Principal principal) {
        if (principal == null)
            return "redirect:/cliente/login";

        Long servicioId = toLong(session.getAttribute("preCita_servicioId"));
        Long barberoId = toLong(session.getAttribute("preCita_barberoId"));

        if (servicioId == null || barberoId == null)
            return "redirect:/reservar";

        // ── VALIDACIÓN TEMPRANA: bloquea antes de mostrar el pago ────────
        String correo = principal.getName();
        com.example.BarberiaLaClasica.model.Cliente clienteLogueado = clienteRepository.findByCorreo(correo).orElse(null);

        if (clienteLogueado != null) {
            clienteLogueado.setTotalVisitas(clienteService.calcularTotalVisitas(clienteLogueado));
            long activas = citaRepository.contarReservasActivasPorCliente(clienteLogueado.getId());
            if (activas >= 1) {
                model.addAttribute("errorReserva",
                        "Ya tienes una reserva activa. Espera a ser atendido antes de hacer una nueva.");
            }
        }

        // ── CÁLCULO DE PROMOCIONES VINCULADAS ───────────────────────────
        servicioRepository.findById(servicioId).ifPresent(s -> {
            model.addAttribute("servicio", s);

            // Calculamos el precio real evaluando si aplica oferta según visitas y uso único por cliente
            double precioFinal = promocionHelper.calcularPrecioServicio(s, clienteLogueado);
            model.addAttribute("precioFinalServicio", precioFinal);
        });

        barberoRepository.findById(barberoId).ifPresent(b -> model.addAttribute("barbero", b));

        model.addAttribute("fecha", session.getAttribute("preCita_fecha"));
        model.addAttribute("hora", session.getAttribute("preCita_hora"));
        model.addAttribute("servicioId", servicioId);
        model.addAttribute("barberoId", barberoId);
        model.addAttribute("config", configuracionSitioService.obtenerMapa());

        return "reserva/reserva-pago";
    }

    // API: Confirma la reserva con el comprobante de pago
    @PostMapping("/cliente/reserva/confirmar")
    public String confirmarReserva(
            @RequestParam Long servicioId,
            @RequestParam Long barberoId,
            @RequestParam String fecha,
            @RequestParam String hora,
            @RequestParam("comprobante") MultipartFile comprobante,
            HttpSession session,
            Principal principal,
            Model model) {

        if (principal == null)
            return "redirect:/cliente/login";

        try {
            citaService.confirmarReserva(
                    principal.getName(), // correo del cliente autenticado
                    servicioId, barberoId, fecha, hora, comprobante);
            // Limpia la sesión
            session.removeAttribute("preCita_servicioId");
            session.removeAttribute("preCita_barberoId");
            session.removeAttribute("preCita_fecha");
            session.removeAttribute("preCita_hora");

            return "redirect:/cliente/mis-citas?exito=true";

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return pantallaPago(session, model, principal);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Historial de citas del cliente
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/cliente/mis-citas")
    public String misCitas(
            @RequestParam(defaultValue = "0") int pagina,
            Model model, Principal principal) {
        if (principal == null)
            return "redirect:/cliente/login";

        Page<Cita> paginaCitas = citaService
                .obtenerHistorialClientePaginado(principal.getName(), pagina);

        model.addAttribute("citas", paginaCitas.getContent());
        model.addAttribute("paginaActual", pagina);
        model.addAttribute("totalPaginas", paginaCitas.getTotalPages());
        List<Map<String, Object>> barberosDto = barberoRepository.findByEstado(1)
                .stream()
                .map(b -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", b.getId());
                    m.put("nombre", b.getNombre());
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());
        model.addAttribute("barberos", barberosDto);
        return "cliente/mis-citas";
    }

    @PostMapping("/cliente/citas/{id}/cancelar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> cancelarCitaCliente(
            @PathVariable Long id, Principal principal) {
        try {
            citaService.cancelarCitaCliente(id, principal.getName());
            return ResponseEntity.ok(Map.of("ok", "true"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/cliente/citas/{id}/reprogramar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> reprogramarCita(
            @PathVariable Long id,
            @RequestBody Map<String, String> datos,
            Principal principal) {
        try {
            citaService.reprogramarCita(
                    id, principal.getName(),
                    Long.parseLong(datos.get("barberoId")),
                    datos.get("fecha"),
                    datos.get("hora"));
            return ResponseEntity.ok(Map.of("ok", "true"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    // ─────────────────────────────────────────────────────────────────
    // Secretario: gestión de citas
    // ─────────────────────────────────────────────────────────────────

    // Utilidad para convertir Object a Long de forma segura
    private Long toLong(Object val) {
        if (val == null)
            return null;
        try {
            return Long.parseLong(val.toString());
        } catch (Exception e) {
            return null;
        }
    }
}