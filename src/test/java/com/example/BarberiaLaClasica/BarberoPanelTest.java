package com.example.BarberiaLaClasica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.BarberiaLaClasica.model.Barbero;
import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.model.RecomendacionIA;
import com.example.BarberiaLaClasica.model.Servicio;
import com.example.BarberiaLaClasica.model.SillaSession;
import com.example.BarberiaLaClasica.repository.BarberoRepository;
import com.example.BarberiaLaClasica.repository.ClienteRepository;
import com.example.BarberiaLaClasica.repository.RecomendacionIARepository;
import com.example.BarberiaLaClasica.repository.ServicioRepository;
import com.example.BarberiaLaClasica.repository.SillaSessionRepository;
import com.example.BarberiaLaClasica.service.BarberoService;

@SpringBootTest
@AutoConfigureMockMvc
class BarberoPanelTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private BarberoService barberoService;
    @Autowired
    private BarberoRepository barberoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ServicioRepository servicioRepository;
    @Autowired
    private SillaSessionRepository sillaSessionRepository;
    @Autowired
    private RecomendacionIARepository recomendacionRepository;

    @Test
    void elBarberoEntraASuPanelYVeElCorteDeSuCliente() throws Exception {
        Barbero barbero = new Barbero();
        barbero.setNombre("Andres Panel");
        barbero = barberoRepository.save(barbero);
        barberoService.asignarCuenta(barbero, "andres.panel@test.pe", "clave123");

        mvc.perform(formLogin("/cliente/login").user("andres.panel@test.pe").password("clave123"))
                .andExpect(authenticated().withRoles("BARBERO"))
                .andExpect(redirectedUrl("/barbero/panel"));

        Cliente c = new Cliente();
        c.setDni("70000003");
        c.setNombres("Mario");
        c.setApellidos("Diaz");
        c.setCorreo("mario.panel@test.pe");
        c = clienteRepository.save(c);
        RecomendacionIA rec = new RecomendacionIA();
        rec.setEstrategia("FEW_SHOT");
        rec.setCliente(c);
        rec.setAprobada(true);
        rec.setCorteRecomendado("Mid fade");
        rec.setFotoUrl("/uploads/ia/fotos/mario.jpg");
        recomendacionRepository.save(rec);

        Servicio servicio = new Servicio();
        servicio.setNombre("Corte panel");
        servicio.setPrecio(BigDecimal.TEN);
        servicio = servicioRepository.save(servicio);
        SillaSession sesion = new SillaSession();
        sesion.setBarbero(barbero);
        sesion.setServicio(servicio);
        sesion.setCliente(c);
        sillaSessionRepository.save(sesion);
        try {
            mvc.perform(get("/barbero/panel").with(
                    org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                            .user("andres.panel@test.pe").roles("BARBERO")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(
                            containsString("Mario Diaz"),
                            containsString("/uploads/ia/fotos/mario.jpg"),
                            containsString("Mid fade"))));
        } finally {
            sillaSessionRepository.deleteAll();
        }
    }

    @Test
    @WithMockUser(roles = "SECRETARIO")
    void elSecretarioNoEntraAlPanelDelBarbero() throws Exception {
        mvc.perform(get("/barbero/panel")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void elAdminCreaLaCuentaDesdeElFormularioDeBarberos() throws Exception {
        mvc.perform(multipart("/admin/barberos/guardar").with(csrf())
                .param("nombre", "Raul Cuenta")
                .param("correo", "raul.cuenta@test.pe")
                .param("password", "clave123"))
                .andExpect(status().is3xxRedirection());
        Barbero raul = barberoService.buscarPorCorreo("raul.cuenta@test.pe").orElseThrow();
        assertThat(raul.getNombre()).isEqualTo("Raul Cuenta");

        // correo repetido: el barbero se guarda pero no se crea otra cuenta
        mvc.perform(multipart("/admin/barberos/guardar").with(csrf())
                .param("nombre", "Otro Barbero")
                .param("correo", "raul.cuenta@test.pe")
                .param("password", "clave123"))
                .andExpect(status().is3xxRedirection());
        assertThat(barberoRepository.findAll()).filteredOn(b -> "Otro Barbero".equals(b.getNombre()))
                .allMatch(b -> b.getUsuario() == null);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void listaDeBarberosNoMuestraLaContrasena() throws Exception {
        mvc.perform(get("/admin/barberos"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("$2a$"))));
    }
}
