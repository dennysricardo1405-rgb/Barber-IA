package com.example.BarberiaLaClasica.service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.BarberiaLaClasica.dto.EstadoMonitoreo;
import com.example.BarberiaLaClasica.dto.LecturaIotRequest;
import com.example.BarberiaLaClasica.model.Barbero;
import com.example.BarberiaLaClasica.model.LecturaIot;
import com.example.BarberiaLaClasica.repository.BarberoRepository;
import com.example.BarberiaLaClasica.repository.LecturaIotRepository;
import com.example.BarberiaLaClasica.repository.SillaSessionRepository;

/**
 * U3 - Capa de plataforma IoT: guarda las lecturas de los sensores, las cruza con
 * las sesiones de recepción y empuja el estado al dashboard en tiempo real (SSE).
 */
@Service
public class MonitoreoIotService {

    private static final Logger log = LoggerFactory.getLogger(MonitoreoIotService.class);

    private final LecturaIotRepository lecturaRepository;
    private final BarberoRepository barberoRepository;
    private final SillaSessionRepository sesionRepository;
    private final List<SseEmitter> emisores = new CopyOnWriteArrayList<>();
    private final AtomicBoolean hayCambios = new AtomicBoolean(false);

    private final LocalTime apertura;
    private final int minSinRegistro;
    private final int minSesionVacia;
    private final int minServicioLargo;
    private final double tempMax;
    private final double humedadMax;
    private final int segundosOffline;

    public MonitoreoIotService(LecturaIotRepository lecturaRepository, BarberoRepository barberoRepository,
            SillaSessionRepository sesionRepository,
            @Value("${iot.horario.apertura:09:00}") LocalTime apertura,
            @Value("${iot.alerta.sin-registro-min:5}") int minSinRegistro,
            @Value("${iot.alerta.sesion-vacia-min:10}") int minSesionVacia,
            @Value("${iot.alerta.servicio-largo-min:60}") int minServicioLargo,
            @Value("${iot.alerta.temperatura-max:28}") double tempMax,
            @Value("${iot.alerta.humedad-max:70}") double humedadMax,
            @Value("${iot.sensor.offline-seg:120}") int segundosOffline) {
        this.lecturaRepository = lecturaRepository;
        this.barberoRepository = barberoRepository;
        this.sesionRepository = sesionRepository;
        this.apertura = apertura;
        this.minSinRegistro = minSinRegistro;
        this.minSesionVacia = minSesionVacia;
        this.minServicioLargo = minServicioLargo;
        this.tempMax = tempMax;
        this.humedadMax = humedadMax;
        this.segundosOffline = segundosOffline;
    }

    // ── Ingesta ──────────────────────────────────────────────────────────────

    @Transactional
    public LecturaIot registrarSilla(Long sillaId, LecturaIotRequest req, String canal) {
        if (sillaId == null || sillaId <= 0 || req.getOcupada() == null) {
            throw new IllegalArgumentException("La lectura de silla requiere un id válido y el campo 'ocupada'");
        }
        LecturaIot l = base(LecturaIot.SILLA, req, canal);
        l.setSillaId(sillaId);
        l.setOcupada(req.getOcupada());
        l.setDistanciaCm(req.getDistanciaCm());
        return guardar(l);
    }

    @Transactional
    public LecturaIot registrarAmbiente(LecturaIotRequest req, String canal) {
        if (req.getTemperatura() == null && req.getHumedad() == null) {
            throw new IllegalArgumentException("La lectura de ambiente requiere temperatura o humedad");
        }
        LecturaIot l = base(LecturaIot.AMBIENTE, req, canal);
        l.setTemperatura(req.getTemperatura());
        l.setHumedad(req.getHumedad());
        return guardar(l);
    }

    private LecturaIot base(String tipo, LecturaIotRequest req, String canal) {
        LecturaIot l = new LecturaIot();
        l.setTipo(tipo);
        l.setDispositivo(req.getDispositivo() == null ? null
                : req.getDispositivo().substring(0, Math.min(40, req.getDispositivo().length())));
        l.setCanal(canal);
        l.setFecha(LocalDateTime.now());
        return l;
    }

    private LecturaIot guardar(LecturaIot l) {
        LecturaIot guardada = lecturaRepository.save(l);
        hayCambios.set(true);
        return guardada;
    }

    // ── Estado para el dashboard ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EstadoMonitoreo estadoActual() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime inicioVentana = LocalDate.now().atTime(apertura);
        if (inicioVentana.isAfter(ahora)) {
            inicioVentana = inicioDia;
        }

        Map<Long, List<LecturaIot>> porSilla = lecturaRepository
                .findByTipoAndFechaGreaterThanEqualOrderByFechaAsc(LecturaIot.SILLA, inicioDia).stream()
                .collect(Collectors.groupingBy(LecturaIot::getSillaId, LinkedHashMap::new, Collectors.toList()));

        // Sillas = barberos habilitados (libres o atendiendo) + cualquier sensor con id desconocido
        Map<Long, String> sillas = new LinkedHashMap<>();
        for (Barbero b : barberoRepository.findAll()) {
            if (b.getEstado() != 0) {
                sillas.put(b.getId(), b.getNombre());
            }
        }
        porSilla.keySet().forEach(id -> sillas.putIfAbsent(id, null));

        List<EstadoMonitoreo.Silla> filas = new ArrayList<>();
        List<String> alertas = new ArrayList<>();
        int ocupadas = 0;
        int servicios = 0;
        int sinRegistro = 0;
        double sumaPct = 0;
        double sumaMinutos = 0;
        int sillasConServicios = 0;

        for (Map.Entry<Long, String> s : sillas.entrySet()) {
            Long id = s.getKey();
            OcupacionCalculator.Resultado r = OcupacionCalculator.calcular(
                    porSilla.getOrDefault(id, List.of()), inicioVentana, ahora);
            boolean online = r.ultimaLectura() != null
                    && Duration.between(r.ultimaLectura(), ahora).getSeconds() <= segundosOffline;
            boolean sesion = sesionRepository.findByBarberoIdAndEstado(id, 1).isPresent();
            long minutos = r.desde() == null ? 0 : Duration.between(r.desde(), ahora).toMinutes();
            String nombre = s.getValue() == null ? "Sin barbero asignado" : s.getValue();

            String alerta = null;
            if (online && Boolean.TRUE.equals(r.ocupadaActual())) {
                if (!sesion && minutos >= minSinRegistro) {
                    alerta = "Ocupada hace " + minutos + " min sin servicio registrado en recepción";
                    sinRegistro++;
                } else if (minutos >= minServicioLargo) {
                    alerta = "El servicio lleva " + minutos + " min";
                }
            } else if (online && Boolean.FALSE.equals(r.ocupadaActual()) && sesion && minutos >= minSesionVacia) {
                alerta = "Cuenta abierta pero la silla está vacía hace " + minutos + " min";
            }
            if (alerta != null) {
                alertas.add("Silla #" + id + " (" + nombre + "): " + alerta);
            }

            double pct = r.porcentaje(inicioVentana, ahora);
            if (Boolean.TRUE.equals(r.ocupadaActual()) && online) {
                ocupadas++;
            }
            servicios += r.servicios();
            sumaPct += pct;
            if (r.minutosPromedioServicio() > 0) {
                sumaMinutos += r.minutosPromedioServicio();
                sillasConServicios++;
            }
            filas.add(new EstadoMonitoreo.Silla(id, nombre, online, r.ocupadaActual(), r.desde(), minutos,
                    sesion, alerta, redondear(pct), r.servicios()));
        }

        List<LecturaIot> ultimasAmbiente = new ArrayList<>(lecturaRepository.findTop60ByTipoOrderByFechaDesc(
                LecturaIot.AMBIENTE));
        Collections.reverse(ultimasAmbiente);
        EstadoMonitoreo.Ambiente ambiente = null;
        if (!ultimasAmbiente.isEmpty()) {
            LecturaIot u = ultimasAmbiente.get(ultimasAmbiente.size() - 1);
            boolean online = Duration.between(u.getFecha(), ahora).getSeconds() <= segundosOffline;
            ambiente = new EstadoMonitoreo.Ambiente(u.getTemperatura(), u.getHumedad(), u.getFecha(), online);
            if (online && u.getTemperatura() != null && u.getTemperatura() > tempMax) {
                alertas.add("Temperatura alta en el local: " + u.getTemperatura() + " °C");
            }
            if (online && u.getHumedad() != null && u.getHumedad() > humedadMax) {
                alertas.add("Humedad alta en el local: " + u.getHumedad() + " %");
            }
        }
        List<EstadoMonitoreo.Punto> serie = ultimasAmbiente.stream()
                .map(l -> new EstadoMonitoreo.Punto(l.getFecha(), l.getTemperatura(), l.getHumedad()))
                .toList();

        EstadoMonitoreo.Kpis kpis = new EstadoMonitoreo.Kpis(ocupadas, sillas.size(),
                sillas.isEmpty() ? 0 : redondear(sumaPct / sillas.size()), servicios,
                sillasConServicios == 0 ? 0 : redondear(sumaMinutos / sillasConServicios), sinRegistro);

        return new EstadoMonitoreo(ahora, filas, ambiente, serie, kpis, alertas);
    }

    private static double redondear(double v) {
        return Math.round(v * 10) / 10.0;
    }

    // ── Tiempo real (Server-Sent Events) ─────────────────────────────────────

    public SseEmitter suscribir() {
        SseEmitter emisor = new SseEmitter(0L);
        emisores.add(emisor);
        emisor.onCompletion(() -> emisores.remove(emisor));
        emisor.onTimeout(() -> emisores.remove(emisor));
        emisor.onError(e -> emisores.remove(emisor));
        try {
            emisor.send(SseEmitter.event().name("estado").data(estadoActual()));
        } catch (IOException e) {
            emisores.remove(emisor);
        }
        return emisor;
    }

    /** Agrupa las lecturas y empuja el estado como máximo cada 2 segundos. */
    @Scheduled(fixedDelay = 2000)
    public void difundir() {
        if (emisores.isEmpty() || !hayCambios.getAndSet(false)) {
            return;
        }
        EstadoMonitoreo estado;
        try {
            estado = estadoActual();
        } catch (RuntimeException e) {
            log.warn("No se pudo calcular el estado IoT: {}", e.getMessage());
            return;
        }
        for (SseEmitter emisor : emisores) {
            try {
                emisor.send(SseEmitter.event().name("estado").data(estado));
            } catch (IOException | IllegalStateException e) {
                emisores.remove(emisor);
            }
        }
    }

    /** Refresco periódico para que los minutos y los sensores offline avancen sin lecturas nuevas. */
    @Scheduled(fixedDelay = 30000)
    public void marcarRefresco() {
        hayCambios.set(true);
    }
}
