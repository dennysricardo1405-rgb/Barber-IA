package com.example.BarberiaLaClasica.controller;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.example.BarberiaLaClasica.dto.ResultadoAsesor;
import com.example.BarberiaLaClasica.model.RecomendacionIA;
import com.example.BarberiaLaClasica.service.AsesorImagenService;
import com.example.BarberiaLaClasica.service.GeminiClient;

/** U2 - Asesor de imagen por IA. */
@Controller
public class AsesorIAController {

    private static final Logger log = LoggerFactory.getLogger(AsesorIAController.class);

    private final AsesorImagenService asesorService;
    private final GeminiClient gemini;

    public AsesorIAController(AsesorImagenService asesorService, GeminiClient gemini) {
        this.asesorService = asesorService;
        this.gemini = gemini;
    }

    @GetMapping("/asesor-ia")
    public String pagina(org.springframework.ui.Model model) {
        model.addAttribute("modoDemo", !gemini.configurado());
        return "asesor-ia";
    }

    @PostMapping("/api/ia/asesor")
    @ResponseBody
    public ResultadoAsesor analizar(@RequestParam("foto") MultipartFile foto,
            @RequestParam(value = "estrategia", required = false) String estrategia,
            @RequestParam(value = "dni", required = false) String dni) throws IOException {
        return asesorService.analizar(foto, estrategia, dni);
    }

    @PostMapping("/api/ia/asesor/{id}/aprobar")
    @ResponseBody
    public Map<String, Object> aprobar(@PathVariable Long id, @RequestParam("dni") String dni) {
        RecomendacionIA rec = asesorService.aprobar(id, dni);
        return Map.of("ok", true, "id", rec.getId(), "corte", String.valueOf(rec.getCorteRecomendado()));
    }

    /** Para recepción: el corte aprobado más reciente del cliente. */
    @GetMapping("/api/ia/cliente/{clienteId}/ultima")
    @ResponseBody
    public ResponseEntity<RecomendacionIA> ultima(@PathVariable Long clienteId) {
        return ResponseEntity.of(asesorService.ultimaAprobada(clienteId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(GeminiClient.GeminiException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> errorIA(GeminiClient.GeminiException e) {
        log.warn("Fallo del asesor IA: {}", e.getMessage());
        return ResponseEntity.status(502).body(Map.of("error", mensajeParaUsuario(e.getStatus())));
    }

    static String mensajeParaUsuario(int status) {
        return switch (status) {
            case 400, 401, 403 -> "Gemini rechazó la clave: revisa que GEMINI_API_KEY sea correcta y esté activa.";
            case 404 -> "El modelo de Gemini configurado no existe: revisa GEMINI_MODEL.";
            case 429 -> "Se alcanzó el límite gratuito de Gemini. Espera un minuto e inténtalo de nuevo.";
            default -> "El asesor de IA no está disponible en este momento. Intenta de nuevo.";
        };
    }
}
