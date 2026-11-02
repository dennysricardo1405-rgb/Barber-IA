package com.example.BarberiaLaClasica.controller;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.BarberiaLaClasica.service.BotAgendaService;
import com.example.BarberiaLaClasica.service.BotException;
import com.example.BarberiaLaClasica.service.BotMetricasService;

/**
 * U4 - Herramientas que usa el agente de citas de n8n. Todas exigen la cabecera X-BOT-KEY.
 * El chat (X-CHAT-ID) lo pone n8n desde el mensaje de Telegram, no la IA, para que no pueda
 * hacerse pasar por otro cliente.
 *
 * Los errores de negocio responden 200 con {"ok": false, "codigo", "mensaje"} para que el agente
 * los lea y se los explique al cliente; los fallos reales responden 5xx y n8n los reintenta.
 */
@RestController
@RequestMapping("/api/bot")
public class BotApiController {

    private static final Logger log = LoggerFactory.getLogger(BotApiController.class);

    private final BotAgendaService agenda;
    private final BotMetricasService metricas;
    private final String apiKey;

    public BotApiController(BotAgendaService agenda, BotMetricasService metricas,
            @Value("${bot.api-key:}") String apiKey) {
        this.agenda = agenda;
        this.metricas = metricas;
        this.apiKey = apiKey;
    }

    public record MensajeRequest(String chatId, String canal) {}

    public record ClienteRequest(String dni, String nombres, String apellidos, String telefono) {}

    public record CitaRequest(String dni, Long servicioId, Long barberoId, String fecha, String hora,
            Boolean confirmado) {}

    public record FalloRequest(String flujo, String nodo, String mensaje) {}

    @PostMapping("/mensajes")
    public ResponseEntity<Map<String, Object>> mensaje(@RequestHeader(value = "X-BOT-KEY", required = false) String key,
            @RequestBody MensajeRequest req) {
        return ejecutar(key, req.chatId(), () -> agenda.registrarMensaje(req.chatId(), req.canal()));
    }

    @GetMapping("/clientes/{dni}")
    public ResponseEntity<Map<String, Object>> buscarCliente(
            @RequestHeader(value = "X-BOT-KEY", required = false) String key,
            @RequestHeader(value = "X-CHAT-ID", required = false) String chatId, @PathVariable String dni) {
        return ejecutar(key, chatId, () -> agenda.buscarCliente(chatId, dni));
    }

    @PostMapping("/clientes")
    public ResponseEntity<Map<String, Object>> registrarCliente(
            @RequestHeader(value = "X-BOT-KEY", required = false) String key,
            @RequestHeader(value = "X-CHAT-ID", required = false) String chatId, @RequestBody ClienteRequest req) {
        return ejecutar(key, chatId,
                () -> agenda.registrarCliente(chatId, req.dni(), req.nombres(), req.apellidos(), req.telefono()));
    }

    @GetMapping("/catalogo")
    public ResponseEntity<Map<String, Object>> catalogo(@RequestHeader(value = "X-BOT-KEY", required = false) String key) {
        return ejecutar(key, null, agenda::catalogo);
    }

    @GetMapping("/horarios")
    public ResponseEntity<Map<String, Object>> horarios(@RequestHeader(value = "X-BOT-KEY", required = false) String key,
            @RequestHeader(value = "X-CHAT-ID", required = false) String chatId,
            @RequestParam(required = false) Long barberoId, @RequestParam(required = false) String fecha,
            @RequestParam(required = false) Long servicioId, @RequestParam(required = false) String hora) {
        return ejecutar(key, chatId, () -> agenda.horarios(chatId, barberoId, fecha, servicioId, hora));
    }

    @PostMapping("/citas")
    public ResponseEntity<Map<String, Object>> crearCita(@RequestHeader(value = "X-BOT-KEY", required = false) String key,
            @RequestHeader(value = "X-CHAT-ID", required = false) String chatId, @RequestBody CitaRequest req) {
        return ejecutar(key, chatId, () -> agenda.crearCita(chatId, req.dni(), req.servicioId(), req.barberoId(),
                req.fecha(), req.hora(), req.confirmado()));
    }

    @PostMapping("/fallos")
    public ResponseEntity<Map<String, Object>> fallo(@RequestHeader(value = "X-BOT-KEY", required = false) String key,
            @RequestBody FalloRequest req) {
        return ejecutar(key, null, () -> {
            agenda.registrarFallo(req.flujo(), req.nodo(), req.mensaje());
            return new LinkedHashMap<>(Map.of("ok", true));
        });
    }

    @GetMapping("/metricas")
    public ResponseEntity<Map<String, Object>> metricas(@RequestHeader(value = "X-BOT-KEY", required = false) String key) {
        return ejecutar(key, null, metricas::calcular);
    }

    // ── Comunes ─────────────────────────────────────────────────────────────

    private interface Accion {
        Map<String, Object> get();
    }

    private ResponseEntity<Map<String, Object>> ejecutar(String key, String chatId, Accion accion) {
        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("ok", false, "codigo", "BOT_DESACTIVADO", "mensaje", "Falta configurar BOT_API_KEY."));
        }
        if (key == null || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8),
                apiKey.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "codigo", "NO_AUTORIZADO", "mensaje", "Clave del bot inválida."));
        }
        try {
            return ResponseEntity.ok(accion.get());
        } catch (BotException e) {
            agenda.registrarErrorControlado(chatId, e);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("ok", false);
            r.put("codigo", e.getCodigo());
            r.put("mensaje", e.getMessage());
            r.putAll(e.getExtra());
            return ResponseEntity.ok(r);
        } catch (RuntimeException e) {
            log.error("Fallo en la API del bot", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ok", false, "codigo", "ERROR_INTERNO",
                            "mensaje", "Tuvimos un problema interno. Intenta de nuevo en unos minutos."));
        }
    }
}
