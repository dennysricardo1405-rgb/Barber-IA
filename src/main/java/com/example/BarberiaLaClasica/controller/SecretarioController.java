package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.model.Cita;
import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.service.CitaService;
import com.example.BarberiaLaClasica.service.ClienteService;
import com.example.BarberiaLaClasica.service.PromocionHelper;
import com.example.BarberiaLaClasica.service.RecepcionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Map;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;

@Controller
@RequestMapping("/secretario")
public class SecretarioController {

    @Autowired
    private CitaService citaService;
    @Autowired
    private ClienteService clienteService;
    @Autowired
    private RecepcionService recepcionService;
    @Autowired
    private PromocionHelper promocionHelper;

    // 1. DASHBOARD DEL SECRETARIO (Vista Principal con la Agenda de Citas)
    @GetMapping("/dashboard")
    public String dashboardSecretario(Model model) {
        List<Cita> citasHoy = citaService.listarCitasDeHoy();
        model.addAttribute("citasHoy", citasHoy);
        model.addAttribute("totalCitasHoy", citasHoy.size());
        model.addAttribute("citasPendientesCount", citaService.listarPendientes().size()); // ← agrega esta línea
        return "secretario/dashboard";
    }

    // Asegúrate de tener inyectado tu servicio de clientes aquí arriba
    // @Autowired private ClienteService clienteService;

    // ── MANTENIMIENTO DE CLIENTES REUTILIZANDO LA VISTA DE TU CONTROLLER ──

    // 1. Listar Clientes
    @GetMapping("/cliente")
    public String listarClientesParaSecretario(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaRegistro").descending());
        Page<Cliente> clientesPage = clienteService.listarTodosPaginado(pageable, search);

        for (Cliente c : clientesPage.getContent()) {
            c.setTotalVisitas(clienteService.calcularTotalVisitas(c));
        }

        model.addAttribute("clientesPage", clientesPage);
        model.addAttribute("clientes", clientesPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", clientesPage.getTotalPages());
        model.addAttribute("totalItems", clientesPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("search", search);
        model.addAttribute("cliente", new Cliente()); // Thymeleaf requiere el objeto vacío para el modal de crear

        return "cliente/clientes-lista";
    }

    // 2. Guardar Cliente desde la Recepción
    @PostMapping("/cliente/guardar")
    public String guardarClienteDesdeSecretario(
            @ModelAttribute Cliente cliente,
            @RequestParam("passwordPlana") String passwordPlana,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            clienteService.crearDesdeAdmin(cliente, passwordPlana);
            ra.addFlashAttribute("exito", "Cliente registrado con éxito.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        // Redirige al mismo entorno del secretario
        return "redirect:/secretario/cliente";
    }

    // 3. Cambiar Estado (Activar/Inactivar)
    @GetMapping("/cliente/estado/{id}")
    public String cambiarEstadoClienteDesdeSecretario(@PathVariable Long id) {
        clienteService.cambiarEstado(id);
        return "redirect:/secretario/cliente";
    }

    // 4. Actualizar Cliente desde el Secretario
    @PostMapping("/cliente/actualizar/{id}")
    public String actualizarClienteDesdeSecretario(@PathVariable Long id,
            @ModelAttribute Cliente cliente,
            @RequestParam(value = "nuevaPassword", required = false) String nuevaPassword,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            String telefono = cliente.getTelefono() != null ? cliente.getTelefono().trim() : "";
            String correo   = cliente.getCorreo()   != null ? cliente.getCorreo().trim()   : "";

            if (!telefono.isEmpty() && !telefono.matches("^\\d{9}$")) {
                ra.addFlashAttribute("error", "El teléfono debe tener exactamente 9 dígitos.");
                return "redirect:/secretario/cliente";
            }
            if (!correo.isEmpty() && !correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                ra.addFlashAttribute("error", "El correo electrónico no es válido.");
                return "redirect:/secretario/cliente";
            }

            if (nuevaPassword != null && !nuevaPassword.trim().isEmpty()) {
                String pwd = nuevaPassword.trim();
                if (pwd.length() < 6) {
                    ra.addFlashAttribute("error", "La contraseña debe tener al menos 6 caracteres.");
                    return "redirect:/secretario/cliente";
                }
                if (pwd.length() > 30) {
                    ra.addFlashAttribute("error", "La contraseña no puede superar 30 caracteres.");
                    return "redirect:/secretario/cliente";
                }
            }

            cliente.setTelefono(telefono.isEmpty() ? null : telefono);
            cliente.setCorreo(correo.isEmpty() ? null : correo);

            clienteService.actualizarDesdeAdmin(id, cliente, nuevaPassword);
            ra.addFlashAttribute("exito", "Datos del cliente actualizados con éxito.");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
        }
        return "redirect:/secretario/cliente";
    }

    @GetMapping("/citas")
    public String gestionCitas(Model model) {
        List<Cita> pendientes = citaService.listarPendientes();
        for (Cita c : pendientes) {
            if (c.getCliente() != null) {
                c.getCliente().setTotalVisitas(clienteService.calcularTotalVisitas(c.getCliente()));
            }
        }
        List<Cita> hoy = citaService.listarCitasDeHoy();
        for (Cita c : hoy) {
            if (c.getCliente() != null) {
                c.getCliente().setTotalVisitas(clienteService.calcularTotalVisitas(c.getCliente()));
            }
        }
        model.addAttribute("citasPendientes", pendientes);
        model.addAttribute("citasHoy", hoy);
        model.addAttribute("promoHelper", promocionHelper);
        return "secretario/citas-gestion";
    }

    // ── Aceptar cita + enviar WhatsApp ────────────────────────────────────────
    @PostMapping("/citas/{id}/aceptar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> aceptarCita(
            @PathVariable Long id,
            @RequestParam(name = "montoYape", defaultValue = "0") java.math.BigDecimal montoYape,
            @RequestParam(name = "montoEfectivo", defaultValue = "0") java.math.BigDecimal montoEfectivo,
            @RequestParam(name = "codigoYape", required = false) String codigoYape) {
        try {
            citaService.aceptarCitaHibridaCompleta(id, montoYape, montoEfectivo, codigoYape);
            return ResponseEntity.ok(Map.of("mensaje", "Cuenta auditada y confirmada con éxito."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Cancelar cita ─────────────────────────────────────────────────────────
    @PostMapping("/citas/{id}/cancelar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> cancelarCita(@PathVariable Long id) {
        try {
            citaService.cancelarCita(id);
            return ResponseEntity.ok(Map.of("mensaje", "Cita cancelada."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/recepcion/api-estado-barbero/{barberoId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> estadoBarbero(
            @PathVariable Long barberoId) {

        Map<String, Object> resp = new HashMap<>();

        recepcionService.getCitaReservaHoy(barberoId).ifPresentOrElse(cita -> {
            LocalTime ahora = LocalTime.now();
            LocalTime horaRes = cita.getHoraInicio();
            long minutos = java.time.Duration.between(ahora, horaRes).toMinutes();

            resp.put("tieneReserva", true);
            resp.put("horaReserva", horaRes.toString());
            resp.put("minutosRestantes", minutos);
            resp.put("bloqueado", minutos >= 0 && minutos <= 30);
            resp.put("cliente", cita.getCliente() != null
                    ? cita.getCliente().getNombres() + " " + cita.getCliente().getApellidos()
                    : "—");
        }, () -> {
            resp.put("tieneReserva", false);
            resp.put("bloqueado", false);
        });

        return ResponseEntity.ok(resp);
    }
}