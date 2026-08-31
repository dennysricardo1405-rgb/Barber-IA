package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.model.Servicio;
import com.example.BarberiaLaClasica.service.ServicioService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/servicios")
public class ServicioController {

    @Autowired
    private ServicioService servicioService;

    @Value("${app.upload.dir:uploads/comprobantes}")
    private String uploadDir;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("servicios", servicioService.listarTodos());
        return "servicios/servicios-lista";
    }

    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute Servicio servicio,
            @RequestParam(value = "imagenFile", required = false) MultipartFile file,
            RedirectAttributes ra) {
        try {
            guardarImagenServicio(servicio, file, null);
            servicioService.guardar(servicio);
            ra.addFlashAttribute("exito", "Servicio guardado con éxito.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/servicios";
    }

    @PostMapping("/editar")
    public String editar(
            @ModelAttribute Servicio servicio,
            @RequestParam(value = "imagenFile", required = false) MultipartFile file,
            RedirectAttributes ra) {
        try {
            // Obtener imagen actual para no perderla si no se sube nueva
            Servicio existente = servicioService.buscarPorId(servicio.getId());
            guardarImagenServicio(servicio, file,
                    existente != null ? existente.getImagenUrl() : null);
            servicioService.guardar(servicio);
            ra.addFlashAttribute("exito", "Servicio actualizado con éxito.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/servicios";
    }

    @GetMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable Long id) {
        servicioService.cambiarEstado(id);
        return "redirect:/admin/servicios";
    }

    // Helper para guardar imagen
    private void guardarImagenServicio(
            Servicio servicio, MultipartFile file, String imagenActual)
            throws IOException {
        if (file != null && !file.isEmpty()) {
            // Igual que productos — usa user.dir
            String dir = System.getProperty("user.dir") + "/uploads/servicios";
            Path ruta = Paths.get(dir);
            Files.createDirectories(ruta);
            String nombre = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), ruta.resolve(nombre));
            servicio.setImagenUrl("/uploads/servicios/" + nombre);
        } else {
            servicio.setImagenUrl(imagenActual);
        }
    }
}