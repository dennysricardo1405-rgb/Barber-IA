package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.service.ClienteService;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/cliente")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @GetMapping
    public String listar(Model model,
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

        return "cliente/clientes-lista";
    }

    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute Cliente cliente,
            @RequestParam("passwordPlana") String passwordPlana,
            RedirectAttributes ra) {
        try {
            clienteService.crearDesdeAdmin(cliente, passwordPlana);
            ra.addFlashAttribute("exito", "Cliente registrado con éxito.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/cliente";
    }

    @PostMapping("/guardar-rapido")
    public ResponseEntity<?> guardarRapido(@RequestBody Map<String, Object> datos) {
        try {
            String dni      = ((String) datos.get("dni") + "").trim();
            String nombres  = ((String) datos.get("nombres") + "").trim();
            String apellidos= ((String) datos.get("apellidos") + "").trim();
            String telefono = datos.get("telefono") != null ? ((String) datos.get("telefono")).trim() : "";
            String correo   = datos.get("correo")   != null ? ((String) datos.get("correo")).trim()   : "";

            if (!dni.matches("^\\d{8}$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "El DNI debe tener exactamente 8 dígitos."));
            }
            if (nombres.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Los nombres son obligatorios."));
            }
            if (apellidos.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Los apellidos son obligatorios."));
            }
            if (!telefono.isEmpty() && !telefono.matches("^\\d{9}$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "El teléfono debe tener exactamente 9 dígitos."));
            }
            if (!correo.isEmpty() && !correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "El correo electrónico no es válido."));
            }

            Cliente c = new Cliente();
            c.setDni(dni);
            c.setNombres(nombres);
            c.setApellidos(apellidos);
            c.setTelefono(telefono.isEmpty() ? null : telefono);
            c.setCorreo(correo.isEmpty() ? null : correo);
            clienteService.crearDesdeAdmin(c, "B" + dni);

            Map<String, Object> resp = new HashMap<>();
            resp.put("id", c.getId());
            resp.put("dni", c.getDni());
            resp.put("nombres", c.getNombres());
            resp.put("apellidos", c.getApellidos());
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/actualizar/{id}")
    public String actualizarCliente(@PathVariable Long id,
                                    @ModelAttribute Cliente cliente,
                                    @RequestParam(value = "nuevaPassword", required = false) String nuevaPassword,
                                    RedirectAttributes ra) {
        try {
            // ── Validaciones backend ──────────────────────────
            String telefono = cliente.getTelefono() != null ? cliente.getTelefono().trim() : "";
            String correo   = cliente.getCorreo()   != null ? cliente.getCorreo().trim()   : "";

            if (!telefono.isEmpty() && !telefono.matches("^\\d{9}$")) {
                ra.addFlashAttribute("error", "El teléfono debe tener exactamente 9 dígitos.");
                return "redirect:/admin/cliente";
            }
            if (!correo.isEmpty() && !correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                ra.addFlashAttribute("error", "El correo electrónico no es válido.");
                return "redirect:/admin/cliente";
            }

            // Validar nueva contraseña si se proporcionó
            if (nuevaPassword != null && !nuevaPassword.trim().isEmpty()) {
                String pwd = nuevaPassword.trim();
                if (pwd.length() < 6) {
                    ra.addFlashAttribute("error", "La contraseña debe tener al menos 6 caracteres.");
                    return "redirect:/admin/cliente";
                }
                if (pwd.length() > 30) {
                    ra.addFlashAttribute("error", "La contraseña no puede superar 30 caracteres.");
                    return "redirect:/admin/cliente";
                }
            }

            cliente.setTelefono(telefono.isEmpty() ? null : telefono);
            cliente.setCorreo(correo.isEmpty() ? null : correo);

            // ← Pasar la nueva contraseña al service
            clienteService.actualizarDesdeAdmin(id, cliente, nuevaPassword);
            ra.addFlashAttribute("exito", "Datos del cliente actualizados con éxito.");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
        }
        return "redirect:/admin/cliente";
    }

    @GetMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable Long id) {
        clienteService.cambiarEstado(id);
        return "redirect:/admin/cliente";
    }
}