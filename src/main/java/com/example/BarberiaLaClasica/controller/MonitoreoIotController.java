package com.example.BarberiaLaClasica.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.BarberiaLaClasica.dto.EstadoMonitoreo;
import com.example.BarberiaLaClasica.dto.LecturaIotRequest;
import com.example.BarberiaLaClasica.model.LecturaIot;
import com.example.BarberiaLaClasica.service.MonitoreoIotService;
import com.example.BarberiaLaClasica.service.MqttIngestService;

/** U3 - Capa de aplicación IoT: dashboard en vivo y entrada HTTP alternativa para los nodos. */
@Controller
public class MonitoreoIotController {

    private final MonitoreoIotService monitoreo;
    private final Optional<MqttIngestService> mqtt;
    private final String tokenHttp;

    public MonitoreoIotController(MonitoreoIotService monitoreo, Optional<MqttIngestService> mqtt,
            @Value("${iot.http.token:}") String tokenHttp) {
        this.monitoreo = monitoreo;
        this.mqtt = mqtt;
        this.tokenHttp = tokenHttp;
    }

    @GetMapping("/monitoreo")
    public String dashboard(Model model) {
        model.addAttribute("activePage", "monitoreo");
        return "monitoreo/dashboard";
    }

    @GetMapping("/monitoreo/api/estado")
    @ResponseBody
    public EstadoMonitoreo estado() {
        return monitoreo.estadoActual();
    }

    @GetMapping("/monitoreo/api/broker")
    @ResponseBody
    public Map<String, Object> broker() {
        return Map.of("mqttHabilitado", mqtt.isPresent(),
                "mqttConectado", mqtt.map(MqttIngestService::conectado).orElse(false));
    }

    @GetMapping(path = "/monitoreo/stream", produces = "text/event-stream")
    @ResponseBody
    public SseEmitter stream() {
        return monitoreo.suscribir();
    }

    // ── Entrada HTTP (alternativa a MQTT para nodos que solo hablan HTTP) ──────

    @PostMapping("/api/iot/sillas/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> lecturaSilla(@PathVariable Long id,
            @RequestHeader(value = "X-IOT-TOKEN", required = false) String token,
            @RequestBody LecturaIotRequest req) {
        if (!tokenValido(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token IoT inválido"));
        }
        LecturaIot l = monitoreo.registrarSilla(id, req, "HTTP");
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", l.getId()));
    }

    @PostMapping("/api/iot/ambiente")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> lecturaAmbiente(
            @RequestHeader(value = "X-IOT-TOKEN", required = false) String token,
            @RequestBody LecturaIotRequest req) {
        if (!tokenValido(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token IoT inválido"));
        }
        LecturaIot l = monitoreo.registrarAmbiente(req, "HTTP");
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", l.getId()));
    }

    private boolean tokenValido(String token) {
        return !tokenHttp.isBlank() && tokenHttp.equals(token);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> invalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
