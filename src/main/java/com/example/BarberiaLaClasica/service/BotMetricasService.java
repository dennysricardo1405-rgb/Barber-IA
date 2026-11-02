package com.example.BarberiaLaClasica.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.BarberiaLaClasica.model.BotConversacion;
import com.example.BarberiaLaClasica.model.BotEvento;
import com.example.BarberiaLaClasica.model.LineaBaseManual;
import com.example.BarberiaLaClasica.repository.BotConversacionRepository;
import com.example.BarberiaLaClasica.repository.BotEventoRepository;
import com.example.BarberiaLaClasica.repository.LineaBaseManualRepository;

/** U4 - Indicadores de eficiencia del agente, calculados de la bitácora y comparados con la línea base manual. */
@Service
public class BotMetricasService {

    private final BotConversacionRepository conversaciones;
    private final BotEventoRepository eventos;
    private final LineaBaseManualRepository lineaBase;
    private final BotAgendaService agenda;
    private final int minutosAbandono;
    private final LocalTime apertura;
    private final LocalTime cierre;

    public BotMetricasService(BotConversacionRepository conversaciones, BotEventoRepository eventos,
            LineaBaseManualRepository lineaBase, BotAgendaService agenda,
            @Value("${bot.minutos-abandono:30}") int minutosAbandono,
            @Value("${bot.horario.apertura:10:00}") String apertura,
            @Value("${bot.horario.cierre:22:00}") String cierre) {
        this.conversaciones = conversaciones;
        this.eventos = eventos;
        this.lineaBase = lineaBase;
        this.agenda = agenda;
        this.minutosAbandono = minutosAbandono;
        this.apertura = LocalTime.parse(apertura);
        this.cierre = LocalTime.parse(cierre);
    }

    public Map<String, Object> calcular() {
        LocalDateTime limiteAbandono = agenda.ahora().minusMinutes(minutosAbandono);
        List<BotConversacion> todas = conversaciones.findAll();

        long creadas = 0, abandonadas = 0, enCurso = 0, fueraDeHorario = 0, mensajes = 0;
        double segundosBot = 0;
        for (BotConversacion c : todas) {
            mensajes += c.getMensajes();
            if (BotConversacion.CITA_CREADA.equals(c.getResultado())) {
                creadas++;
                segundosBot += Duration.between(c.getInicio(), c.getFin()).toSeconds();
                LocalTime h = c.getInicio().toLocalTime();
                if (h.isBefore(apertura) || !h.isBefore(cierre)) {
                    fueraDeHorario++;
                }
            } else if (c.getUltimaActividad().isBefore(limiteAbandono)) {
                abandonadas++;
            } else {
                enCurso++;
            }
        }
        long cerradas = creadas + abandonadas;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("conversaciones", todas.size());
        m.put("citasCreadas", creadas);
        m.put("abandonadas", abandonadas);
        m.put("enCurso", enCurso);
        m.put("mensajes", mensajes);
        m.put("citasFueraDeHorario", fueraDeHorario);
        m.put("porcentajeSinIntervencion", cerradas == 0 ? null : redondear(100.0 * creadas / cerradas));
        Double minutosBot = creadas == 0 ? null : redondear(segundosBot / 60.0 / creadas);
        m.put("minutosPromedioBot", minutosBot);

        List<LineaBaseManual> base = lineaBase.findAll();
        Double atencionManual = base.isEmpty() ? null
                : redondear(base.stream().mapToDouble(LineaBaseManual::getMinutosAtencion).average().orElse(0));
        Double totalManual = base.isEmpty() ? null
                : redondear(base.stream().mapToDouble(LineaBaseManual::getMinutosTotal).average().orElse(0));
        m.put("muestrasLineaBase", base.size());
        m.put("minutosAtencionManual", atencionManual);
        m.put("minutosTotalManual", totalManual);
        m.put("minutosPersonalAhorrados", atencionManual == null ? null : redondear(atencionManual * creadas));
        m.put("reduccionTiempoPorcentaje", (totalManual == null || minutosBot == null || totalManual == 0) ? null
                : redondear(100.0 * (totalManual - minutosBot) / totalManual));

        Map<String, Long> porCodigo = new TreeMap<>();
        for (BotEvento e : eventos.findByTipo(BotEvento.ERROR_CONTROLADO)) {
            porCodigo.merge(e.getCodigo(), 1L, Long::sum);
        }
        m.put("erroresControlados", porCodigo.values().stream().mapToLong(Long::longValue).sum());
        m.put("erroresPorCodigo", porCodigo);
        m.put("fallosNoControlados", eventos.countByTipo(BotEvento.FALLO));
        return m;
    }

    private static double redondear(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
