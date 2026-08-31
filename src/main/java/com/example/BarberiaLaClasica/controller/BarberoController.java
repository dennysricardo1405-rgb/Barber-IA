package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.model.Barbero;
import com.example.BarberiaLaClasica.model.Cita;
import com.example.BarberiaLaClasica.model.NotaVenta;
import com.example.BarberiaLaClasica.repository.CitaRepository;
import com.example.BarberiaLaClasica.repository.NotaVentaRepository;
import com.example.BarberiaLaClasica.service.BarberoService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/barberos")
public class BarberoController {

    @Autowired
    private BarberoService barberoService;
    @Autowired
    private CitaRepository citaRepository;
    @Autowired
    private NotaVentaRepository notaVentaRepository;
    private static final String[] DIAS = { "MARTES", "MIERCOLES" };

    // ── Lista con Paginación ─────────────────────────────────────
    @GetMapping
    public String lista(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
        Page<Barbero> barberosPage = barberoService.buscar(search, pageable);

        model.addAttribute("barberos", barberosPage.getContent());
        model.addAttribute("barberosPage", barberosPage);
        model.addAttribute("dias", DIAS);
        model.addAttribute("search", search);

        return "barberos/lista";
    }

    // ── Guardar nuevo ────────────────────────────────────────────
    @PostMapping("/guardar")
    public String guardar(
            @RequestParam String nombre,
            @RequestParam(required = false) String especialidad,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String diaLibre,
            @RequestParam(value = "foto", required = false) MultipartFile foto,
            RedirectAttributes redirect) {
        try {
            // ── Validaciones backend ──
            nombre = nombre != null ? nombre.trim() : "";
            if (nombre.isEmpty()) {
                redirect.addFlashAttribute("error", "El nombre es obligatorio.");
                return "redirect:/admin/barberos";
            }
            if (nombre.length() < 3 || nombre.length() > 80) {
                redirect.addFlashAttribute("error", "El nombre debe tener entre 3 y 80 caracteres.");
                return "redirect:/admin/barberos";
            }
            if (!nombre.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) {
                redirect.addFlashAttribute("error", "El nombre solo puede contener letras.");
                return "redirect:/admin/barberos";
            }

            if (especialidad != null && !especialidad.trim().isEmpty()) {
                especialidad = especialidad.trim();
                if (!especialidad.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) {
                    redirect.addFlashAttribute("error", "La especialidad solo puede contener letras.");
                    return "redirect:/admin/barberos";
                }
                if (especialidad.length() > 60) {
                    redirect.addFlashAttribute("error", "La especialidad no puede superar 60 caracteres.");
                    return "redirect:/admin/barberos";
                }
            }

            if (telefono != null && !telefono.trim().isEmpty()) {
                telefono = telefono.trim();
                if (!telefono.matches("^\\d{9}$")) {
                    redirect.addFlashAttribute("error", "El teléfono debe tener exactamente 9 dígitos.");
                    return "redirect:/admin/barberos";
                }
            }

            Barbero b = new Barbero();
            b.setNombre(nombre);
            b.setEspecialidad(especialidad != null ? especialidad.trim() : null);
            b.setTelefono(telefono != null ? telefono.trim() : null);
            b.setDiaLibre(diaLibre != null ? diaLibre : "MARTES");
            barberoService.guardar(b, foto);
            redirect.addFlashAttribute("exito", "Barbero registrado correctamente ✓");

        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al registrar: " + e.getMessage());
        }
        return "redirect:/admin/barberos";
    }

    // ── Actualizar ───────────────────────────────────────────────
    @PostMapping("/{id}/actualizar")
    public String actualizar(
            @PathVariable Long id,
            @RequestParam String nombre,
            @RequestParam(required = false) String especialidad,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String diaLibre,
            @RequestParam(value = "foto", required = false) MultipartFile foto,
            RedirectAttributes redirect) {
        try {
            // ── Validaciones backend ──
            nombre = nombre != null ? nombre.trim() : "";
            if (nombre.isEmpty()) {
                redirect.addFlashAttribute("error", "El nombre es obligatorio.");
                return "redirect:/admin/barberos";
            }
            if (nombre.length() < 3 || nombre.length() > 80) {
                redirect.addFlashAttribute("error", "El nombre debe tener entre 3 y 80 caracteres.");
                return "redirect:/admin/barberos";
            }
            if (!nombre.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) {
                redirect.addFlashAttribute("error", "El nombre solo puede contener letras.");
                return "redirect:/admin/barberos";
            }

            if (especialidad != null && !especialidad.trim().isEmpty()) {
                especialidad = especialidad.trim();
                if (!especialidad.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) {
                    redirect.addFlashAttribute("error", "La especialidad solo puede contener letras.");
                    return "redirect:/admin/barberos";
                }
                if (especialidad.length() > 60) {
                    redirect.addFlashAttribute("error", "La especialidad no puede superar 60 caracteres.");
                    return "redirect:/admin/barberos";
                }
            }

            if (telefono != null && !telefono.trim().isEmpty()) {
                telefono = telefono.trim();
                if (!telefono.matches("^\\d{9}$")) {
                    redirect.addFlashAttribute("error", "El teléfono debe tener exactamente 9 dígitos.");
                    return "redirect:/admin/barberos";
                }
            }

            Barbero datos = new Barbero();
            datos.setNombre(nombre);
            datos.setEspecialidad(especialidad != null ? especialidad.trim() : null);
            datos.setTelefono(telefono != null ? telefono.trim() : null);
            datos.setDiaLibre(diaLibre != null ? diaLibre : "MARTES");
            datos.setEstado(1);
            barberoService.actualizar(id, datos, foto);
            redirect.addFlashAttribute("exito", "Barbero actualizado correctamente ✓");

        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
        }
        return "redirect:/admin/barberos";
    }

    // ── Cambiar estado ───────────────────────────────────────────
    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            barberoService.cambiarEstado(id);
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al cambiar estado");
        }
        return "redirect:/admin/barberos";
    }

    @GetMapping("/sueldos")
    public String vistasSueldos(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio,
            Model model) {

        YearMonth periodo = (mes != null && anio != null)
                ? YearMonth.of(anio, mes)
                : YearMonth.now();

        List<Barbero> barberos = barberoService.listarTodos();
        Map<Barbero, Map<String, Object>> resumen = new LinkedHashMap<>();

        for (Barbero b : barberos) {
            // ← Ahora usa NotaVenta en vez de Cita
            List<NotaVenta> notas = notaVentaRepository
                    .findByBarberoAndPeriodo(b, periodo.getYear(), periodo.getMonthValue());

            double totalGenerado = notas.stream()
                    .mapToDouble(NotaVenta::getTotal)
                    .sum();

            double comision = totalGenerado * 0.50;

            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("notas", notas);
            datos.put("totalNotas", notas.size());
            datos.put("totalGenerado", totalGenerado);
            datos.put("comision", comision);

            resumen.put(b, datos);
        }

        List<YearMonth> mesesDisponibles = new ArrayList<>();
        for (int i = 0; i < 12; i++)
            mesesDisponibles.add(YearMonth.now().minusMonths(i));

        double totalGeneralPeriodo = resumen.values().stream()
                .mapToDouble(d -> (double) d.get("totalGenerado")).sum();
        double totalComisionesPeriodo = resumen.values().stream()
                .mapToDouble(d -> (double) d.get("comision")).sum();
        int totalAtencionesPeriodo = resumen.values().stream()
                .mapToInt(d -> (int) d.get("totalNotas")).sum();

        model.addAttribute("resumen", resumen);
        model.addAttribute("periodo", periodo);
        model.addAttribute("mesesDisponibles", mesesDisponibles);
        model.addAttribute("totalGeneralPeriodo", totalGeneralPeriodo);
        model.addAttribute("totalComisionesPeriodo", totalComisionesPeriodo);
        model.addAttribute("totalCitasPeriodo", totalAtencionesPeriodo);

        return "barberos/sueldos";
    }
}