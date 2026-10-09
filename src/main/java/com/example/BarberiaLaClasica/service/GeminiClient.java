package com.example.BarberiaLaClasica.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * U2 - Cliente HTTP de la API de Google Gemini (Google AI Studio).
 * Usa la API REST generateContent directamente desde Spring Boot.
 */
@Component
public class GeminiClient {

    public record Imagen(byte[] datos, String mimeType) {
    }

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String modeloTexto;
    private final String modeloImagen;
    private final String modeloRespaldo;

    public GeminiClient(RestClient.Builder builder, String baseUrl, String apiKey, String modeloTexto,
            String modeloImagen) {
        this(builder, baseUrl, apiKey, modeloTexto, modeloImagen, "");
    }

    @Autowired
    public GeminiClient(RestClient.Builder builder,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-3.8-flash}") String modeloTexto,
            @Value("${gemini.image-model:gemini-3.1-flash-lite-image}") String modeloImagen,
            @Value("${gemini.fallback-model:gemini-3.5-flash-lite}") String modeloRespaldo) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.modeloTexto = modeloTexto;
        this.modeloImagen = modeloImagen;
        this.modeloRespaldo = modeloRespaldo == null ? "" : modeloRespaldo.trim();
    }

    /** Sin API key el sistema trabaja en modo demostración. */
    public boolean configurado() {
        return !apiKey.isEmpty();
    }

    public String modeloTexto() {
        return modeloTexto;
    }

    /**
     * Envía la instrucción de sistema, el prompt y la foto, y pide la respuesta en JSON.
     *
     * @return el texto JSON devuelto por el modelo
     */
    public String analizarImagenJson(String sistema, String prompt, Imagen foto, double temperatura) {
        Map<String, Object> cuerpo = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", sistema))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(
                        Map.of("text", prompt),
                        parteImagen(foto)))),
                "generationConfig", Map.of(
                        "temperature", temperatura,
                        "responseMimeType", "application/json"));

        JsonNode respuesta;
        try {
            respuesta = llamar(modeloTexto, cuerpo);
        } catch (GeminiException e) {
            // 503 = modelo saturado: se intenta una vez con el modelo de respaldo
            if (e.getStatus() != 503 || modeloRespaldo.isEmpty() || modeloRespaldo.equals(modeloTexto)) {
                throw e;
            }
            log.warn("{} saturado, se usa {}", modeloTexto, modeloRespaldo);
            respuesta = llamar(modeloRespaldo, cuerpo);
        }
        StringBuilder texto = new StringBuilder();
        for (JsonNode parte : partes(respuesta)) {
            if (parte.hasNonNull("text")) {
                texto.append(parte.get("text").asText());
            }
        }
        if (texto.isEmpty()) {
            throw new GeminiException("Gemini no devolvió texto: " + resumen(respuesta));
        }
        return texto.toString();
    }

    /** Pide al modelo de imágenes que edite la foto con el corte indicado. */
    public Imagen editarImagen(String prompt, Imagen foto) {
        Map<String, Object> cuerpo = Map.of(
                "contents", List.of(Map.of("role", "user", "parts", List.of(
                        Map.of("text", prompt),
                        parteImagen(foto)))),
                "generationConfig", Map.of("responseModalities", List.of("TEXT", "IMAGE")));

        JsonNode respuesta = llamar(modeloImagen, cuerpo);
        for (JsonNode parte : partes(respuesta)) {
            JsonNode inline = parte.has("inlineData") ? parte.get("inlineData") : parte.get("inline_data");
            if (inline != null && inline.hasNonNull("data")) {
                String mime = inline.has("mimeType") ? inline.get("mimeType").asText() : "image/png";
                return new Imagen(Base64.getDecoder().decode(inline.get("data").asText()), mime);
            }
        }
        throw new GeminiException("Gemini no devolvió una imagen: " + resumen(respuesta));
    }

    private JsonNode llamar(String modelo, Map<String, Object> cuerpo) {
        if (!configurado()) {
            throw new GeminiException("GEMINI_API_KEY no está configurada");
        }
        try {
            return restClient.post()
                    .uri("/models/{modelo}:generateContent", modelo)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            throw new GeminiException("Gemini respondió " + status + " (" + modelo + "): "
                    + mensajeDeGoogle(e.getResponseBodyAsString()), status, e);
        } catch (RuntimeException e) {
            throw new GeminiException("Error llamando a Gemini (" + modelo + "): " + e.getMessage(), e);
        }
    }

    /** Google devuelve {"error": {"message": "..."}}; si no, se usa el cuerpo recortado. */
    private static String mensajeDeGoogle(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            return "sin detalle";
        }
        try {
            String mensaje = new com.fasterxml.jackson.databind.ObjectMapper().readTree(cuerpo)
                    .path("error").path("message").asText("");
            if (!mensaje.isBlank()) {
                return mensaje;
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException ignorada) {
            // no era JSON
        }
        return cuerpo.length() > 300 ? cuerpo.substring(0, 300) + "..." : cuerpo;
    }

    private static Map<String, Object> parteImagen(Imagen foto) {
        return Map.of("inlineData", Map.of(
                "mimeType", foto.mimeType(),
                "data", Base64.getEncoder().encodeToString(foto.datos())));
    }

    private static List<JsonNode> partes(JsonNode respuesta) {
        List<JsonNode> lista = new ArrayList<>();
        if (respuesta == null) {
            return lista;
        }
        JsonNode parts = respuesta.path("candidates").path(0).path("content").path("parts");
        parts.forEach(lista::add);
        return lista;
    }

    private static String resumen(JsonNode respuesta) {
        if (respuesta == null) {
            return "respuesta vacía";
        }
        String s = respuesta.toString();
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }

    public static class GeminiException extends RuntimeException {
        /** Código HTTP devuelto por Google, o 0 si el fallo no vino de una respuesta HTTP. */
        private final int status;

        public GeminiException(String mensaje) {
            this(mensaje, 0, null);
        }

        public GeminiException(String mensaje, Throwable causa) {
            this(mensaje, 0, causa);
        }

        public GeminiException(String mensaje, int status, Throwable causa) {
            super(mensaje, causa);
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
}
