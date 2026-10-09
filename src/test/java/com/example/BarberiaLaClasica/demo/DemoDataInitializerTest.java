package com.example.BarberiaLaClasica.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.example.BarberiaLaClasica.repository.BarberoRepository;
import com.example.BarberiaLaClasica.repository.ClienteRepository;
import com.example.BarberiaLaClasica.repository.ServicioRepository;

/** Con DEMO_DATA=true la BD vacía arranca con datos coherentes y con historial hacia atrás. */
@SpringBootTest(properties = {
        "app.demo-data=true",
        "spring.datasource.url=jdbc:h2:mem:demo;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class DemoDataInitializerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private BarberoRepository barberoRepository;
    @Autowired
    private ServicioRepository servicioRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void cargaCatalogoYClientes() {
        assertThat(barberoRepository.count()).isEqualTo(4);
        assertThat(servicioRepository.count()).isEqualTo(6);
        assertThat(clienteRepository.count()).isEqualTo(20);
    }

    @Test
    void ventasRepartidasEnLosUltimosMeses() {
        LocalDateTime primera = jdbc.queryForObject("select min(fecha) from notas_venta", LocalDateTime.class);
        Integer ventas = jdbc.queryForObject("select count(*) from notas_venta", Integer.class);
        assertThat(ventas).isGreaterThan(200);
        assertThat(primera.toLocalDate()).isBefore(LocalDate.now().minusDays(60));
    }

    @Test
    void hayCitasConfirmadasParaHoy() {
        Integer hoy = jdbc.queryForObject("select count(*) from citas where fecha = ? and estado = 2",
                Integer.class, LocalDate.now());
        assertThat(hoy).isGreaterThanOrEqualTo(3);
    }

    @Test
    void usuariosDeDemoPuedenIniciarSesion() throws Exception {
        mvc.perform(formLogin("/cliente/login").user("secretario@gmail.com").password("secretario123"))
                .andExpect(authenticated().withRoles("SECRETARIO"));
        mvc.perform(formLogin("/cliente/login").user("juan.perez@gmail.com").password("cliente123"))
                .andExpect(authenticated().withRoles("CLIENTE"));
    }
}
