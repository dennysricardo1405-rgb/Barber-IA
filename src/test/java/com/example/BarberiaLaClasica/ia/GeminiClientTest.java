package com.example.BarberiaLaClasica.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.example.BarberiaLaClasica.service.GeminiClient;

class GeminiClientTest {

    private static final String BASE = "https://gemini.test/v1beta";
    private final GeminiClient.Imagen foto = new GeminiClient.Imagen(new byte[] { 1, 2, 3 }, "image/jpeg");

    @Test
    void enviaPromptImagenYPideJson() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiClient cliente = new GeminiClient(builder, BASE, "clave", "modelo-texto", "modelo-imagen");

        server.expect(requestTo(BASE + "/models/modelo-texto:generateContent"))
                .andExpect(header("x-goog-api-key", "clave"))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value("sistema"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("prompt"))
                .andExpect(jsonPath("$.contents[0].parts[1].inlineData.mimeType").value("image/jpeg"))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\"rostroDetectado\\":true}"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(cliente.analizarImagenJson("sistema", "prompt", foto, 0.4)).isEqualTo("{\"rostroDetectado\":true}");
        server.verify();
    }

    @Test
    void extraeLaImagenGenerada() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiClient cliente = new GeminiClient(builder, BASE, "clave", "modelo-texto", "modelo-imagen");
        String png = Base64.getEncoder().encodeToString(new byte[] { 9, 8, 7 });

        server.expect(requestTo(BASE + "/models/modelo-imagen:generateContent"))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"Listo"},
                          {"inlineData":{"mimeType":"image/png","data":"%s"}}]}}]}
                        """.formatted(png), MediaType.APPLICATION_JSON));

        GeminiClient.Imagen imagen = cliente.editarImagen("prompt", foto);
        assertThat(imagen.mimeType()).isEqualTo("image/png");
        assertThat(imagen.datos()).containsExactly(9, 8, 7);
    }

    @Test
    void sinApiKeyNoLlamaALaApi() {
        GeminiClient cliente = new GeminiClient(RestClient.builder(), BASE, " ", "m", "mi");
        assertThat(cliente.configurado()).isFalse();
        assertThatThrownBy(() -> cliente.analizarImagenJson("s", "p", foto, 0.4))
                .isInstanceOf(GeminiClient.GeminiException.class);
    }

    @Test
    void errorDeGoogleConservaCodigoYMensaje() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiClient cliente = new GeminiClient(builder, BASE, "clave", "modelo-texto", "modelo-imagen");
        server.expect(requestTo(BASE + "/models/modelo-texto:generateContent"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":429,\"message\":\"Quota exceeded\"}}"));

        assertThatThrownBy(() -> cliente.analizarImagenJson("s", "p", foto, 0.4))
                .isInstanceOfSatisfying(GeminiClient.GeminiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(429);
                    assertThat(e.getMessage()).contains("Quota exceeded");
                });
    }
}
