package com.example.BarberiaLaClasica.iot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.BarberiaLaClasica.dto.EstadoMonitoreo;
import com.example.BarberiaLaClasica.model.Barbero;
import com.example.BarberiaLaClasica.model.LecturaIot;
import com.example.BarberiaLaClasica.repository.BarberoRepository;
import com.example.BarberiaLaClasica.repository.LecturaIotRepository;
import com.example.BarberiaLaClasica.service.MonitoreoIotService;
import com.example.BarberiaLaClasica.service.MqttIngestService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class MonitoreoIotTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private MonitoreoIotService monitoreo;
    @Autowired
    private LecturaIotRepository lecturaRepository;
    @Autowired
    private BarberoRepository barberoRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private Barbero barbero;

    @BeforeEach
    void preparar() {
        lecturaRepository.deleteAll();
        barbero = new Barbero();
        barbero.setNombre("Carlos IoT");
        barbero = barberoRepository.save(barbero);
    }

    @Test
    void nodoHttpConTokenRegistraLaLectura() throws Exception {
        mvc.perform(post("/api/iot/sillas/" + barbero.getId())
                .header("X-IOT-TOKEN", "token-de-prueba")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ocupada\":true,\"distanciaCm\":35.5,\"dispositivo\":\"esp32-test\"}"))
                .andExpect(status().isCreated());
        assertThat(lecturaRepository.count()).isEqualTo(1);
    }

    @Test
    void nodoHttpSinTokenEsRechazado() throws Exception {
        mvc.perform(post("/api/iot/ambiente")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"temperatura\":24}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lecturaIncompletaDevuelve400() throws Exception {
        mvc.perform(post("/api/iot/sillas/" + barbero.getId())
                .header("X-IOT-TOKEN", "token-de-prueba")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"distanciaCm\":35}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mqttEnrutaLosTopicosDeSillaYAmbiente() {
        MqttIngestService mqtt = new MqttIngestService(monitoreo, objectMapper, "tcp://localhost:1", "laclasica", "",
                "");
        mqtt.procesar("laclasica/sillas/" + barbero.getId() + "/estado", "{\"ocupada\":true}");
        mqtt.procesar("laclasica/ambiente", "{\"temperatura\":31.2,\"humedad\":55}");
        mqtt.procesar("laclasica/otro", "{\"ocupada\":true}");
        mqtt.procesar("laclasica/sillas/abc/estado", "no es json");

        assertThat(lecturaRepository.findAll()).extracting(LecturaIot::getTipo)
                .containsExactlyInAnyOrder(LecturaIot.SILLA, LecturaIot.AMBIENTE);
    }

    @Test
    void sillaOcupadaSinServicioEnCajaGeneraAlerta() {
        MqttIngestService mqtt = new MqttIngestService(monitoreo, objectMapper, "tcp://localhost:1", "laclasica", "",
                "");
        mqtt.procesar("laclasica/sillas/" + barbero.getId() + "/estado", "{\"ocupada\":true}");
        mqtt.procesar("laclasica/ambiente", "{\"temperatura\":31.2,\"humedad\":55}");

        EstadoMonitoreo estado = monitoreo.estadoActual();
        EstadoMonitoreo.Silla silla = estado.sillas().stream()
                .filter(s -> s.id().equals(barbero.getId())).findFirst().orElseThrow();
        assertThat(silla.ocupada()).isTrue();
        assertThat(silla.sensorOnline()).isTrue();
        assertThat(silla.sesionEnSistema()).isFalse();
        assertThat(silla.alerta()).contains("sin servicio registrado");
        assertThat(estado.kpis().sillasSinRegistro()).isEqualTo(1);
        assertThat(estado.alertas()).anyMatch(a -> a.startsWith("Temperatura alta"));
    }

    @Test
    void dashboardRequiereLogin() throws Exception {
        mvc.perform(get("/monitoreo")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "SECRETARIO")
    void secretarioVeElDashboardYElEstado() throws Exception {
        mvc.perform(get("/monitoreo")).andExpect(status().isOk());
        mvc.perform(get("/monitoreo/api/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.sillasTotales").isNumber());
    }

    @Test
    @WithMockUser(roles = "SECRETARIO")
    void recepcionMuestraElBadgeDelSensor() throws Exception {
        mvc.perform(get("/secretario/recepcion"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("badge-iot")));
    }
}
