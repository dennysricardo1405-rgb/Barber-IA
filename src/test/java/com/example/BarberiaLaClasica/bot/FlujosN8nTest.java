package com.example.BarberiaLaClasica.bot;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** U4 - Los flujos de n8n versionados en el repo están bien armados y apuntan a la API del bot. */
class FlujosN8nTest {

    private final ObjectMapper json = new ObjectMapper();

    private JsonNode leer(String archivo) throws Exception {
        return json.readTree(Files.readString(Path.of("n8n/workflows", archivo)));
    }

    @Test
    void elAgenteTieneSusHerramientasYUsaElFlujoDeErrores() throws Exception {
        JsonNode agente = leer("agente-de-citas.json");
        assertThat(agente.path("settings").path("errorWorkflow").asText())
                .isEqualTo(leer("errores-del-bot.json").path("id").asText());

        Set<String> nombres = new HashSet<>();
        List<String> herramientas = new ArrayList<>();
        for (JsonNode n : agente.path("nodes")) {
            nombres.add(n.path("name").asText());
            if (n.path("type").asText().equals("n8n-nodes-base.httpRequestTool")) {
                herramientas.add(n.path("name").asText());
                assertThat(n.path("parameters").path("url").asText()).contains("/api/bot/");
                assertThat(n.path("parameters").path("headerParameters").toString())
                        .contains("X-BOT-KEY").contains("X-CHAT-ID");
            }
        }
        assertThat(herramientas).containsExactlyInAnyOrder("buscar_cliente", "registrar_cliente", "ver_catalogo",
                "ver_horarios", "crear_cita");

        // Todas las conexiones apuntan a nodos que existen
        agente.path("connections").fields().forEachRemaining(origen -> {
            assertThat(nombres).contains(origen.getKey());
            origen.getValue().forEach(tipo -> tipo.forEach(salida -> salida.forEach(
                    destino -> assertThat(nombres).contains(destino.path("node").asText()))));
        });
    }

    @Test
    void elAgenteNoGuardaSecretosEnElFlujo() throws Exception {
        for (String archivo : List.of("agente-de-citas.json", "errores-del-bot.json")) {
            String texto = Files.readString(Path.of("n8n/workflows", archivo));
            assertThat(texto).doesNotContainPattern("AIza[0-9A-Za-z_-]{20,}") // clave de Google
                    .doesNotContainPattern("\\d{8,10}:[0-9A-Za-z_-]{30,}"); // token de Telegram
        }
    }
}
