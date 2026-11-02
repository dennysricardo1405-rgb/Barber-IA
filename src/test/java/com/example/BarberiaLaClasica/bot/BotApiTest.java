package com.example.BarberiaLaClasica.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.example.BarberiaLaClasica.model.Barbero;
import com.example.BarberiaLaClasica.model.BotEvento;
import com.example.BarberiaLaClasica.model.Cita;
import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.model.Servicio;
import com.example.BarberiaLaClasica.repository.BarberoRepository;
import com.example.BarberiaLaClasica.repository.BotEventoRepository;
import com.example.BarberiaLaClasica.repository.CitaRepository;
import com.example.BarberiaLaClasica.repository.ClienteRepository;
import com.example.BarberiaLaClasica.repository.LineaBaseManualRepository;
import com.example.BarberiaLaClasica.repository.ServicioRepository;
import com.example.BarberiaLaClasica.service.DniService;

/** U4 - Reglas del agente de citas: la API rechaza lo inválido aunque la IA lo pida. */
@SpringBootTest
@AutoConfigureMockMvc
class BotApiTest {

    private static final String CLAVE = "clave-bot-prueba";
    private static final AtomicInteger SECUENCIA = new AtomicInteger(10);

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private BarberoRepository barberoRepository;
    @Autowired
    private ServicioRepository servicioRepository;
    @Autowired
    private CitaRepository citaRepository;
    @Autowired
    private BotEventoRepository eventoRepository;
    @Autowired
    private LineaBaseManualRepository lineaBaseRepository;

    @MockitoBean
    private DniService dniService;

    private Barbero barbero;
    private Servicio servicio;
    private Cliente cliente;
    private String chat;
    private String manana;

    @BeforeEach
    void preparar() {
        when(dniService.consultarDni(anyString())).thenReturn(
                "{\"success\":true,\"datos\":{\"nombres\":\"ANA\",\"ape_paterno\":\"PEREZ\",\"ape_materno\":\"RUIZ\"}}");

        barbero = new Barbero();
        barbero.setNombre("Carlos Bot");
        barbero.setDiaLibre("NINGUNO");
        barbero = barberoRepository.save(barbero);

        servicio = new Servicio();
        servicio.setNombre("Corte clásico");
        servicio.setPrecio(new BigDecimal("25.00"));
        servicio.setDuracionMinutos(30);
        servicio = servicioRepository.save(servicio);

        cliente = nuevoCliente(dniNuevo());
        chat = "chat-" + UUID.randomUUID();
        manana = LocalDate.now(ZoneId.of("America/Lima")).plusDays(1).toString();
    }

    @Test
    void sinClaveSeRechaza() throws Exception {
        mvc.perform(get("/api/bot/catalogo")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/bot/catalogo").header("X-BOT-KEY", "otra")).andExpect(status().isUnauthorized());
    }

    @Test
    void dniInvalidoEsUnErrorControladoYQuedaEnLaBitacora() throws Exception {
        long antes = eventoRepository.countByTipo(BotEvento.ERROR_CONTROLADO);
        bot(get("/api/bot/clientes/123ab"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.codigo").value("DNI_INVALIDO"));
        assertThat(eventoRepository.countByTipo(BotEvento.ERROR_CONTROLADO)).isEqualTo(antes + 1);
    }

    @Test
    void agendaCompletaSoloConConfirmacionYSinDuplicar() throws Exception {
        bot(post("/api/bot/mensajes").content("{\"chatId\":\"" + chat + "\",\"canal\":\"telegram\"}"))
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.clienteConocido").value(false));

        bot(get("/api/bot/clientes/" + cliente.getDni()))
                .andExpect(jsonPath("$.existe").value(true));
        assertThat(clienteRepository.findById(cliente.getId()).orElseThrow().getTelegramChatId()).isEqualTo(chat);

        bot(get("/api/bot/catalogo"))
                .andExpect(jsonPath("$.servicios[*].nombre", hasItem("Corte clásico")));

        bot(get("/api/bot/horarios").param("barberoId", "" + barbero.getId()).param("fecha", manana)
                .param("servicioId", "" + servicio.getId()))
                .andExpect(jsonPath("$.horasLibres", hasItem("10:00")));

        String cita = cita(cliente.getDni(), "10:00", false);
        bot(post("/api/bot/citas").content(cita))
                .andExpect(jsonPath("$.codigo").value("SIN_CONFIRMACION"));

        String confirmada = cita(cliente.getDni(), "10:00", true);
        bot(post("/api/bot/citas").content(confirmada))
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.codigo", Matchers.startsWith("LC-")))
                .andExpect(jsonPath("$.precio").value(25.0));

        // Doble "sí" o reintento de n8n: devuelve la misma cita
        bot(post("/api/bot/citas").content(confirmada))
                .andExpect(jsonPath("$.yaRegistrada").value(true));

        assertThat(citaRepository.findByClienteIdAndEstadoInOrderByFechaAscHoraInicioAsc(cliente.getId(),
                java.util.List.of(1, 2))).hasSize(1).first()
                .extracting(Cita::getOrigen).isEqualTo("BOT");

        // El cliente queda reconocido por su chat
        bot(post("/api/bot/mensajes").content("{\"chatId\":\"" + chat + "\"}"))
                .andExpect(jsonPath("$.clienteConocido").value(true))
                .andExpect(jsonPath("$.dni").value(cliente.getDni()));
    }

    @Test
    void horarioOcupadoOfreceAlternativas() throws Exception {
        Cliente otro = nuevoCliente(dniNuevo());
        bot(post("/api/bot/citas").content(cita(otro.getDni(), "15:00", true)))
                .andExpect(jsonPath("$.ok").value(true));

        mvc.perform(post("/api/bot/citas").header("X-BOT-KEY", CLAVE).header("X-CHAT-ID", "otro-" + chat)
                .contentType(MediaType.APPLICATION_JSON).content(cita(cliente.getDni(), "15:00", true)))
                .andExpect(jsonPath("$.codigo").value("HORARIO_OCUPADO"))
                .andExpect(jsonPath("$.alternativas", hasItem("14:30")));
    }

    @Test
    void validaFechasHorasYDiaLibre() throws Exception {
        String ayer = LocalDate.now(ZoneId.of("America/Lima")).minusDays(1).toString();
        bot(get("/api/bot/horarios").param("barberoId", "" + barbero.getId()).param("fecha", ayer))
                .andExpect(jsonPath("$.codigo").value("FECHA_PASADA"));
        bot(get("/api/bot/horarios").param("barberoId", "" + barbero.getId()).param("fecha", "mañana"))
                .andExpect(jsonPath("$.codigo").value("FECHA_INVALIDA"));
        bot(post("/api/bot/citas").content(cita(cliente.getDni(), "23:00", true)))
                .andExpect(jsonPath("$.codigo").value("FUERA_DE_HORARIO"));
        bot(post("/api/bot/citas").content(cita(cliente.getDni(), "10:15", true)))
                .andExpect(jsonPath("$.codigo").value("FUERA_DE_HORARIO"));
        bot(get("/api/bot/horarios").param("barberoId", "99999").param("fecha", manana))
                .andExpect(jsonPath("$.codigo").value("BARBERO_INVALIDO"));

        LocalDate libre = LocalDate.now(ZoneId.of("America/Lima")).plusDays(2);
        barbero.setDiaLibre(new String[] { "LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO" }
                [libre.getDayOfWeek().getValue() - 1]);
        barberoRepository.save(barbero);
        bot(get("/api/bot/horarios").param("barberoId", "" + barbero.getId()).param("fecha", libre.toString()))
                .andExpect(jsonPath("$.codigo").value("DIA_LIBRE_BARBERO"));
    }

    @Test
    void registraClienteNuevoConDatosDeReniec() throws Exception {
        String dni = dniNuevo();
        bot(get("/api/bot/clientes/" + dni))
                .andExpect(jsonPath("$.existe").value(false))
                .andExpect(jsonPath("$.nombresReniec").value("ANA"));

        bot(post("/api/bot/clientes").content("{\"dni\":\"" + dni + "\",\"telefono\":\"12345\"}"))
                .andExpect(jsonPath("$.codigo").value("TELEFONO_INVALIDO"));

        bot(post("/api/bot/clientes").content("{\"dni\":\"" + dni + "\",\"telefono\":\"+51 987 654 321\"}"))
                .andExpect(jsonPath("$.registrado").value(true))
                .andExpect(jsonPath("$.nombre").value("ANA PEREZ RUIZ"));

        Cliente nuevo = clienteRepository.findByDni(dni).orElseThrow();
        assertThat(nuevo.getTelefono()).isEqualTo("987654321");
        assertThat(nuevo.getTelegramChatId()).isEqualTo(chat);
    }

    @Test
    void unClienteSoloPuedeTenerUnaCitaActiva() throws Exception {
        bot(post("/api/bot/citas").content(cita(cliente.getDni(), "11:00", true)))
                .andExpect(jsonPath("$.ok").value(true));
        mvc.perform(post("/api/bot/citas").header("X-BOT-KEY", CLAVE).header("X-CHAT-ID", "nuevo-" + chat)
                .contentType(MediaType.APPLICATION_JSON).content(cita(cliente.getDni(), "12:00", true)))
                .andExpect(jsonPath("$.codigo").value("CITA_ACTIVA_EXISTENTE"))
                .andExpect(jsonPath("$.citasActivas[0].hora").value("11:00"));
    }

    @Test
    void falloDeN8nYMetricas() throws Exception {
        bot(post("/api/bot/fallos").content("{\"flujo\":\"Agente\",\"nodo\":\"Gemini\",\"mensaje\":\"timeout\"}"))
                .andExpect(jsonPath("$.ok").value(true));
        assertThat(eventoRepository.countByTipo(BotEvento.FALLO)).isPositive();

        bot(get("/api/bot/metricas"))
                .andExpect(jsonPath("$.fallosNoControlados", Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void panelDeAutomatizacionGuardaLaLineaBase() throws Exception {
        long antes = lineaBaseRepository.count();
        mvc.perform(post("/admin/automatizacion/linea-base").with(csrf())
                .param("fecha", manana).param("minutosAtencion", "6").param("minutosTotal", "25"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/automatizacion/linea-base").with(csrf())
                .param("fecha", manana).param("minutosAtencion", "30").param("minutosTotal", "5"))
                .andExpect(status().is3xxRedirection());
        assertThat(lineaBaseRepository.count()).isEqualTo(antes + 1);

        mvc.perform(get("/admin/automatizacion"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Antes y después")));
    }

    // ── Apoyo ───────────────────────────────────────────────────────────────

    private ResultActions bot(MockHttpServletRequestBuilder req) throws Exception {
        return mvc.perform(req.header("X-BOT-KEY", CLAVE).header("X-CHAT-ID", chat)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private String cita(String dni, String hora, boolean confirmado) {
        return String.format(
                "{\"dni\":\"%s\",\"servicioId\":%d,\"barberoId\":%d,\"fecha\":\"%s\",\"hora\":\"%s\",\"confirmado\":%s}",
                dni, servicio.getId(), barbero.getId(), manana, hora, confirmado);
    }

    private Cliente nuevoCliente(String dni) {
        Cliente c = new Cliente();
        c.setDni(dni);
        c.setNombres("Luis");
        c.setApellidos("Torres");
        return clienteRepository.save(c);
    }

    private static String dniNuevo() {
        return String.format("7%07d", SECUENCIA.incrementAndGet() + (int) (System.nanoTime() % 1000) * 1000);
    }
}
