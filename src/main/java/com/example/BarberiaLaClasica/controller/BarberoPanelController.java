package com.example.BarberiaLaClasica.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.BarberiaLaClasica.dto.CorteIAVista;
import com.example.BarberiaLaClasica.model.Barbero;
import com.example.BarberiaLaClasica.model.Cita;
import com.example.BarberiaLaClasica.model.NotaVenta;
import com.example.BarberiaLaClasica.model.SillaSession;
import com.example.BarberiaLaClasica.repository.CitaRepository;
import com.example.BarberiaLaClasica.repository.NotaVentaRepository;
import com.example.BarberiaLaClasica.service.AsesorImagenService;
import com.example.BarberiaLaClasica.service.BarberoService;
import com.example.BarberiaLaClasica.service.RecepcionService;

/** Panel del barbero: el cliente en su silla, sus reservas de hoy y sus ganancias del mes. */
@Controller
@RequestMapping("/barbero")
public class BarberoPanelController {

    // mismo porcentaje que usa el reporte de sueldos
    private static final double COMISION = 0.50;

    private final BarberoService barberoService;
    private final RecepcionService recepcionService;
    private final AsesorImagenService asesorImagenService;
    private final CitaRepository citaRepository;
    private final NotaVentaRepository notaVentaRepository;

    public BarberoPanelController(BarberoService barberoService, RecepcionService recepcionService,
            AsesorImagenService asesorImagenService, CitaRepository citaRepository,
            NotaVentaRepository notaVentaRepository) {
        this.barberoService = barberoService;
        this.recepcionService = recepcionService;
        this.asesorImagenService = asesorImagenService;
        this.citaRepository = citaRepository;
        this.notaVentaRepository = notaVentaRepository;
    }

    @GetMapping("/panel")
    public String panel(Principal principal, Model model) {
        Optional<Barbero> encontrado = barberoService.buscarPorCorreo(principal.getName());
        if (encontrado.isEmpty()) {
            model.addAttribute("sinBarbero", true);
            return "barbero/panel";
        }
        Barbero barbero = encontrado.get();
        LocalDate hoy = LocalDate.now();

        SillaSession sesion = recepcionService.getSessionActiva(barbero.getId()).orElse(null);
        CorteIAVista corteActual = sesion != null && sesion.getCliente() != null
                ? asesorImagenService.vistaParaBarbero(sesion.getCliente().getId()).orElse(null)
                : null;

        // reservas pendientes (1) o confirmadas (2) de hoy, con el corte IA de cada cliente si lo tiene
        List<Cita> reservas = citaRepository.findByFechaOrderByHoraInicioAsc(hoy).stream()
                .filter(c -> c.getBarbero() != null && c.getBarbero().getId().equals(barbero.getId()))
                .filter(c -> c.getEstado() == 1 || c.getEstado() == 2)
                .toList();
        Map<Long, CorteIAVista> cortesReserva = new HashMap<>();
        for (Cita c : reservas) {
            if (c.getCliente() != null) {
                asesorImagenService.vistaParaBarbero(c.getCliente().getId())
                        .ifPresent(v -> cortesReserva.put(c.getId(), v));
            }
        }

        List<NotaVenta> notasMes = notaVentaRepository.findByBarberoAndPeriodo(barbero, hoy.getYear(),
                hoy.getMonthValue());
        double totalMes = notasMes.stream().mapToDouble(NotaVenta::getTotal).sum();
        long atencionesHoy = notasMes.stream()
                .filter(n -> n.getFecha() != null && n.getFecha().toLocalDate().equals(hoy))
                .count();

        model.addAttribute("barbero", barbero);
        model.addAttribute("sesion", sesion);
        model.addAttribute("corteActual", corteActual);
        model.addAttribute("reservas", reservas);
        model.addAttribute("cortesReserva", cortesReserva);
        model.addAttribute("atencionesMes", notasMes.size());
        model.addAttribute("atencionesHoy", atencionesHoy);
        model.addAttribute("totalMes", totalMes);
        model.addAttribute("comisionMes", totalMes * COMISION);
        model.addAttribute("ultimasNotas", notasMes.stream()
                .sorted(Comparator.comparing(NotaVenta::getFecha, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .toList());
        return "barbero/panel";
    }
}
