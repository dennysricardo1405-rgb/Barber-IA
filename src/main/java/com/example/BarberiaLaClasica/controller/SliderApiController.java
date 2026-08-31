package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.model.SliderImage;
import com.example.BarberiaLaClasica.service.SliderImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * API REST para gestión del slider.
 * Vista: /admin/slider   (GET - devuelve la página Thymeleaf)
 * API:   /api/slider      (CRUD JSON para el JS del frontend)
 */
@RestController
@RequestMapping("/api/slider")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class SliderApiController {

    private final SliderImageService service;

    public SliderApiController(SliderImageService service) {
        // Inyección de dependencias por constructor
        this.service = service;
    }

    // ── GET /api/slider → lista todas (para el panel de administración) ───────
    @GetMapping
    public ResponseEntity<List<SliderImage>> listar() {
        return ResponseEntity.ok(service.listarTodas());
    }

    // ── GET /api/slider/activas → solo activas (carrusel público de la barbería) ──
    @GetMapping("/activas")
    @PreAuthorize("permitAll()") // Permitir acceso a visitantes sin loguear
    public ResponseEntity<List<SliderImage>> listarActivas() {
        return ResponseEntity.ok(service.listarActivas());
    }

    // ── POST /api/slider/subir → sube y procesa una nueva imagen ─────────
    @PostMapping("/subir")
    public ResponseEntity<?> subir(
            @RequestParam("archivo")              MultipartFile archivo,
            @RequestParam(value = "titulo",      defaultValue = "") String titulo,
            @RequestParam(value = "descripcion", defaultValue = "") String descripcion,
            @RequestParam(value = "orden",       defaultValue = "0") Integer orden) {

        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest().body("Debes seleccionar una imagen.");
        }

        // Validar tamaño máximo de 5 MB
        if (archivo.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body("La imagen supera el límite de 5 MB.");
        }

        // Validar tipos MIME permitidos
        String mime = archivo.getContentType();
        if (mime == null || (!mime.startsWith("image/jpeg")
                          && !mime.startsWith("image/png")
                          && !mime.startsWith("image/webp"))) {
            return ResponseEntity.badRequest().body("Formato no permitido. Usa JPG, PNG o WEBP.");
        }

        try {
            SliderImage guardada = service.guardar(archivo, titulo, descripcion, orden);
            return ResponseEntity.ok(guardada);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al guardar: " + e.getMessage());
        }
    }

    // ── POST /api/slider/{id}/toggle → cambia el estado (activo/inactivo) ──
    @PostMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(@PathVariable Long id) {
        try {
            service.toggleActivo(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── DELETE /api/slider/{id} → elimina físicamente la imagen y su registro ──
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            service.eliminar(id);
            return ResponseEntity.noContent().build();   // Código HTTP 204
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();    // Código HTTP 404
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}