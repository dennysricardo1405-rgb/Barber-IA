package com.example.BarberiaLaClasica.controller;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.BarberiaLaClasica.model.LineaBaseManual;
import com.example.BarberiaLaClasica.repository.BotConversacionRepository;
import com.example.BarberiaLaClasica.repository.BotEventoRepository;
import com.example.BarberiaLaClasica.repository.LineaBaseManualRepository;
import com.example.BarberiaLaClasica.service.BotMetricasService;

/** U4 - Panel de eficiencia del agente de citas: antes (manual) frente a después (bot). */
@Controller
public class AutomatizacionController {

    private final BotMetricasService metricas;
    private final BotConversacionRepository conversaciones;
    private final BotEventoRepository eventos;
    private final LineaBaseManualRepository lineaBase;

    public AutomatizacionController(BotMetricasService metricas, BotConversacionRepository conversaciones,
            BotEventoRepository eventos, LineaBaseManualRepository lineaBase) {
        this.metricas = metricas;
        this.conversaciones = conversaciones;
        this.eventos = eventos;
        this.lineaBase = lineaBase;
    }

    @GetMapping("/admin/automatizacion")
    public String panel(Model model) {
        model.addAttribute("activePage", "automatizacion");
        model.addAttribute("m", metricas.calcular());
        model.addAttribute("conversaciones", conversaciones.findTop20ByOrderByInicioDesc());
        model.addAttribute("eventos", eventos.findTop20ByOrderByFechaDesc());
        model.addAttribute("lineaBase", lineaBase.findAllByOrderByFechaDescIdDesc());
        model.addAttribute("hoy", LocalDate.now());
        return "admin/automatizacion";
    }

    @PostMapping("/admin/automatizacion/linea-base")
    public String agregarMedicion(@RequestParam LocalDate fecha, @RequestParam double minutosAtencion,
            @RequestParam double minutosTotal, @RequestParam(required = false) String nota, RedirectAttributes ra) {
        if (minutosAtencion <= 0 || minutosTotal <= 0 || minutosAtencion > minutosTotal) {
            ra.addFlashAttribute("error",
                    "Los minutos deben ser mayores que cero y la atención no puede superar el tiempo total.");
            return "redirect:/admin/automatizacion";
        }
        LineaBaseManual l = new LineaBaseManual();
        l.setFecha(fecha);
        l.setMinutosAtencion(minutosAtencion);
        l.setMinutosTotal(minutosTotal);
        l.setNota(nota == null || nota.isBlank() ? null : nota.trim());
        lineaBase.save(l);
        ra.addFlashAttribute("ok", "Medición manual guardada.");
        return "redirect:/admin/automatizacion";
    }

    @PostMapping("/admin/automatizacion/linea-base/{id}/eliminar")
    public String eliminarMedicion(@PathVariable Long id, RedirectAttributes ra) {
        lineaBase.deleteById(id);
        ra.addFlashAttribute("ok", "Medición eliminada.");
        return "redirect:/admin/automatizacion";
    }

    /** Borra la bitácora del bot (conversaciones y eventos) para medir desde cero. No toca citas ni la línea base. */
    @PostMapping("/admin/automatizacion/reiniciar")
    public String reiniciar(RedirectAttributes ra) {
        eventos.deleteAllInBatch();
        conversaciones.deleteAllInBatch();
        ra.addFlashAttribute("ok", "Bitácora del bot limpiada. Los indicadores empiezan desde cero.");
        return "redirect:/admin/automatizacion";
    }
}
