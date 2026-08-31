package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.model.HistorialInventario;
import com.example.BarberiaLaClasica.repository.HistorialInventarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class KardexController {

    @Autowired
    private HistorialInventarioRepository inventarioRepository;

    @GetMapping("/inventario/movimientos")
    public String verMovimientos(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String mesFiltro) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
        Page<HistorialInventario> movimientosPage;

        LocalDateTime inicio = null;
        LocalDateTime fin = null;

        // ── 1. COMPROBAMOS SI SE FILTRÓ POR UN MES ESPECÍFICO (Formato: "YYYY-MM") ──
        if (mesFiltro != null && !mesFiltro.trim().isEmpty()) {
            YearMonth ym = YearMonth.parse(mesFiltro);
            inicio = ym.atDay(1).atStartOfDay();
            fin = ym.atEndOfMonth().atTime(LocalTime.MAX);
        } 
        // ── 2. SI NO HAY MES, VERIFICAMOS RANGO DE CALENDARIO DIARIO ──
        else if (fechaInicio != null && fechaFin != null) {
            inicio = fechaInicio.atStartOfDay();
            fin = fechaFin.atTime(LocalTime.MAX);
        }

        // Ejecutamos la consulta en base a si existen límites de tiempo estructurados
        if (inicio != null && fin != null) {
            movimientosPage = inventarioRepository.findByFechaBetween(inicio, fin, pageable);
        } else {
            movimientosPage = inventarioRepository.findAll(pageable);
        }

        List<HistorialInventario> listaMovimientos = movimientosPage.getContent();

        // Totales calculados en caliente basándonos estrictamente en los registros filtrados
        long entradas = listaMovimientos.stream().filter(m -> "ENTRADA".equals(m.getTipoMovimiento())).count();
        long salidas = listaMovimientos.stream().filter(m -> "SALIDA".equals(m.getTipoMovimiento())).count();

        // Atributos de datos a Thymeleaf
        model.addAttribute("movimientosPage", movimientosPage);
        model.addAttribute("movimientos", listaMovimientos);
        model.addAttribute("totalEntradas", entradas);
        model.addAttribute("totalSalidas", salidas);

        // Recordatorios de filtros aplicados en la barra de interfaz
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);
        model.addAttribute("mesFiltro", mesFiltro);

        // Atributos obligatorios para la barra de paginación inferior
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", movimientosPage.getTotalPages());
        model.addAttribute("totalItems", movimientosPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("activePage", "kardex");

        return "admin/inventario-movimientos";
    }
}