package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cliente")
public class ClientePublicoController {

    @Autowired
    private ClienteService clienteService;

    // ── Login ─────────────────────────────────────────────
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // ── Registro ──────────────────────────────────────────
    @GetMapping("/registro")
    public String registroPage() {
        return "cliente/cliente-registro"; // ← así como se llama el archivo
    }

    @PostMapping("/registro")
    public String registrar(
            @ModelAttribute Cliente cliente,
            @RequestParam("passwordPlana") String passwordPlana,
            Model model,
            RedirectAttributes ra) {
        try {
            clienteService.registrarOnline(cliente, passwordPlana);
            ra.addFlashAttribute("exito", "¡Cuenta creada con éxito! Ya puedes iniciar sesión.");
            return "redirect:/cliente/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "cliente/cliente-registro";
        }
    }

}