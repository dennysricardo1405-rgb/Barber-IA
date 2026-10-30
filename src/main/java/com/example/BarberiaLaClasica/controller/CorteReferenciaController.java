package com.example.BarberiaLaClasica.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.BarberiaLaClasica.service.CorteReferenciaService;

/** Catálogo de cortes con foto de referencia que usa el asesor IA. */
@Controller
@RequestMapping("/admin/cortes")
public class CorteReferenciaController {

    private final CorteReferenciaService service;

    public CorteReferenciaController(CorteReferenciaService service) {
        this.service = service;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("cortes", service.listarTodos());
        return "cortes/lista";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam(required = false) Long id,
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam(value = "foto", required = false) MultipartFile foto,
            RedirectAttributes redirect) {
        try {
            service.guardar(id, nombre, descripcion, foto);
            redirect.addFlashAttribute("exito", "Corte guardado correctamente.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "No se pudo guardar la foto: " + e.getMessage());
        }
        return "redirect:/admin/cortes";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id) {
        service.cambiarEstado(id);
        return "redirect:/admin/cortes";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        service.eliminar(id);
        redirect.addFlashAttribute("exito", "Corte eliminado.");
        return "redirect:/admin/cortes";
    }
}
