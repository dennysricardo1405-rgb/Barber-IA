package com.example.BarberiaLaClasica.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.model.RecomendacionIA;
import com.example.BarberiaLaClasica.repository.ClienteRepository;
import com.example.BarberiaLaClasica.repository.RecomendacionIARepository;
import com.example.BarberiaLaClasica.service.GeminiClient;

@SpringBootTest
@AutoConfigureMockMvc
class AsesorIAControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private RecomendacionIARepository recomendacionRepository;
    @MockitoBean
    private GeminiClient gemini;

    private final MockMultipartFile selfie = new MockMultipartFile("foto", "selfie.jpg", "image/jpeg",
            new byte[] { (byte) 0xFF, (byte) 0xD8, 1, 2, 3 });

    @Test
    void paginaEsPublica() throws Exception {
        mvc.perform(get("/asesor-ia")).andExpect(status().isOk());
    }

    @Test
    void sinApiKeyRespondeEnModoDemo() throws Exception {
        when(gemini.configurado()).thenReturn(false);
        mvc.perform(multipart("/api/ia/asesor").file(selfie).param("estrategia", "ZERO_SHOT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demo").value(true))
                .andExpect(jsonPath("$.estrategia").value("ZERO_SHOT"))
                .andExpect(jsonPath("$.recomendacion.cortes.length()").value(3));
    }

    @Test
    void conGeminiParseaElJsonYGuardaLaVistaPrevia() throws Exception {
        when(gemini.configurado()).thenReturn(true);
        when(gemini.modeloTexto()).thenReturn("gemini-test");
        // El modelo a veces envuelve el JSON en un bloque de código; el servicio debe tolerarlo
        when(gemini.analizarImagenJson(anyString(), anyString(), any(), anyDouble())).thenReturn("""
                ```json
                {"rostroDetectado":true,"formaRostro":"CUADRADO","rasgos":["mandíbula angular"],
                 "cortes":[{"nombre":"Crew cut","motivo":"Resalta la mandíbula","mantenimientoSemanas":3}],
                 "corteRecomendado":"Crew cut","servicioSugerido":"Corte clásico",
                 "promptImagen":"crew cut","mensaje":"Te queda un Crew cut"}
                ```""");
        when(gemini.editarImagen(anyString(), any())).thenReturn(new GeminiClient.Imagen(new byte[] { 1 }, "image/png"));

        String json = mvc.perform(multipart("/api/ia/asesor").file(selfie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demo").value(false))
                .andExpect(jsonPath("$.estrategia").value("FEW_SHOT"))
                .andExpect(jsonPath("$.recomendacion.formaRostro").value("CUADRADO"))
                .andExpect(jsonPath("$.previewUrl").value(org.hamcrest.Matchers.endsWith(".png")))
                .andReturn().getResponse().getContentAsString();
        Long id = com.jayway.jsonpath.JsonPath.parse(json).read("$.id", Long.class);

        RecomendacionIA guardada = recomendacionRepository.findById(id).orElseThrow();
        assertThat(guardada.getMantenimientoSemanas()).isEqualTo(3);
        assertThat(guardada.getModelo()).isEqualTo("gemini-test");
    }

    @Test
    void rechazaArchivosQueNoSonImagen() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("foto", "doc.pdf", "application/pdf", new byte[] { 1 });
        mvc.perform(multipart("/api/ia/asesor").file(pdf))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser(roles = "SECRETARIO")
    void elClienteApruebaConSuDniYRecepcionLoVe() throws Exception {
        when(gemini.configurado()).thenReturn(false);
        Cliente c = new Cliente();
        c.setDni("70000001");
        c.setNombres("Juan");
        c.setApellidos("Pérez");
        c.setCorreo("juan.ia@test.pe");
        c = clienteRepository.save(c);

        String json = mvc.perform(multipart("/api/ia/asesor").file(selfie))
                .andReturn().getResponse().getContentAsString();
        Long id = com.jayway.jsonpath.JsonPath.parse(json).read("$.id", Long.class);

        mvc.perform(post("/api/ia/asesor/" + id + "/aprobar").param("dni", "70000001"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/ia/cliente/" + c.getId() + "/ultima"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.corteRecomendado").value("Pompadour clásico"))
                .andExpect(jsonPath("$.aprobada").value(true));
    }

    @Test
    void aprobarConDniDesconocidoDevuelve400() throws Exception {
        when(gemini.configurado()).thenReturn(false);
        String json = mvc.perform(multipart("/api/ia/asesor").file(selfie))
                .andReturn().getResponse().getContentAsString();
        Long id = com.jayway.jsonpath.JsonPath.parse(json).read("$.id", Long.class);
        mvc.perform(post("/api/ia/asesor/" + id + "/aprobar").param("dni", "99999999"))
                .andExpect(status().isBadRequest());
    }
}
