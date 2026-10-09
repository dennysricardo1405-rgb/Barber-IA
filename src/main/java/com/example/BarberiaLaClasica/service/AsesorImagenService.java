package com.example.BarberiaLaClasica.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.BarberiaLaClasica.dto.RecomendacionCorte;
import com.example.BarberiaLaClasica.dto.ResultadoAsesor;
import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.model.RecomendacionIA;
import com.example.BarberiaLaClasica.repository.ClienteRepository;
import com.example.BarberiaLaClasica.repository.RecomendacionIARepository;
import com.example.BarberiaLaClasica.repository.ServicioRepository;
import com.example.BarberiaLaClasica.service.PromptLibrary.Estrategia;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * U2 - Asesor de imagen: analiza la selfie del cliente con Gemini, recomienda el corte
 * y genera una vista previa con el corte aplicado.
 */
@Service
public class AsesorImagenService {

    private static final Logger log = LoggerFactory.getLogger(AsesorImagenService.class);
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_BYTES = 5 * 1024 * 1024;

    private final GeminiClient gemini;
    private final PromptLibrary prompts;
    private final ServicioRepository servicioRepository;
    private final ClienteRepository clienteRepository;
    private final RecomendacionIARepository recomendacionRepository;
    private final ObjectMapper objectMapper;
    private final boolean generarPreview;
    private final String carpetaUploads;

    public AsesorImagenService(GeminiClient gemini, PromptLibrary prompts,
            ServicioRepository servicioRepository, ClienteRepository clienteRepository,
            RecomendacionIARepository recomendacionRepository, ObjectMapper objectMapper,
            @Value("${gemini.preview.enabled:true}") boolean generarPreview,
            @Value("${app.uploads.root:uploads}") String carpetaUploads) {
        this.gemini = gemini;
        this.prompts = prompts;
        this.servicioRepository = servicioRepository;
        this.clienteRepository = clienteRepository;
        this.recomendacionRepository = recomendacionRepository;
        this.objectMapper = objectMapper;
        this.generarPreview = generarPreview;
        this.carpetaUploads = carpetaUploads;
    }

    @Transactional
    public ResultadoAsesor analizar(MultipartFile foto, String estrategiaTexto, String dni) throws IOException {
        validar(foto);
        Estrategia estrategia = Estrategia.desde(estrategiaTexto);
        GeminiClient.Imagen imagen = new GeminiClient.Imagen(foto.getBytes(), foto.getContentType());
        String fotoUrl = guardar(imagen, "fotos");

        long inicio = System.currentTimeMillis();
        boolean demo = !gemini.configurado();
        RecomendacionCorte recomendacion;
        String previewUrl = null;

        if (demo) {
            recomendacion = recomendacionDemo();
            previewUrl = fotoUrl;
        } else {
            String json = gemini.analizarImagenJson(
                    prompts.sistemaAsesor(),
                    prompts.usuarioAsesor(estrategia, catalogo()),
                    imagen, 0.4);
            recomendacion = parsear(json);
            if (generarPreview && recomendacion.isRostroDetectado() && recomendacion.getCorteRecomendado() != null) {
                previewUrl = generarVistaPrevia(recomendacion, imagen);
            }
        }
        long latencia = System.currentTimeMillis() - inicio;

        RecomendacionIA entidad = new RecomendacionIA();
        entidad.setEstrategia(estrategia.name());
        entidad.setModelo(demo ? "demo" : gemini.modeloTexto());
        entidad.setDemo(demo);
        entidad.setFormaRostro(recomendacion.getFormaRostro());
        entidad.setCorteRecomendado(recomendacion.getCorteRecomendado());
        entidad.setServicioSugerido(recomendacion.getServicioSugerido());
        entidad.setMantenimientoSemanas(recomendacion.mantenimientoDelRecomendado());
        entidad.setResultadoJson(objectMapper.writeValueAsString(recomendacion));
        entidad.setFotoUrl(fotoUrl);
        entidad.setPreviewUrl(previewUrl);
        entidad.setLatenciaMs(latencia);
        if (dni != null && dni.matches("\\d{8}")) {
            entidad.setDni(dni);
            clienteRepository.findByDni(dni).ifPresent(entidad::setCliente);
        }
        recomendacionRepository.save(entidad);

        return new ResultadoAsesor(entidad.getId(), entidad.getEstrategia(), entidad.getModelo(), demo,
                latencia, fotoUrl, previewUrl, recomendacion);
    }

    /** El cliente aprueba el corte con su DNI; queda vinculado para que el barbero lo vea. */
    @Transactional
    public RecomendacionIA aprobar(Long id, String dni) {
        if (dni == null || !dni.matches("\\d{8}")) {
            throw new IllegalArgumentException("El DNI debe tener 8 dígitos");
        }
        RecomendacionIA rec = recomendacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recomendación no encontrada"));
        Cliente cliente = clienteRepository.findByDni(dni)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay un cliente registrado con ese DNI. Regístrate o reserva primero."));
        rec.setDni(dni);
        rec.setCliente(cliente);
        rec.setAprobada(true);
        return recomendacionRepository.save(rec);
    }

    public Optional<RecomendacionIA> ultimaAprobada(Long clienteId) {
        return recomendacionRepository.findFirstByClienteIdAndAprobadaTrueOrderByFechaRegistroDesc(clienteId);
    }

    RecomendacionCorte parsear(String json) {
        String limpio = json.trim();
        // Algunos modelos envuelven el JSON en ```json ... ``` aunque se les pida lo contrario
        if (limpio.startsWith("```")) {
            limpio = limpio.replaceFirst("^```(json)?", "").replaceFirst("```$", "").trim();
        }
        try {
            return objectMapper.readValue(limpio, RecomendacionCorte.class);
        } catch (JsonProcessingException e) {
            throw new GeminiClient.GeminiException("La respuesta de la IA no es un JSON válido", e);
        }
    }

    String catalogo() {
        List<String> nombres = servicioRepository.findByEstado(1).stream()
                .map(s -> "- " + s.getNombre() + (s.getDescripcion() != null ? ": " + s.getDescripcion() : ""))
                .collect(Collectors.toList());
        return nombres.isEmpty() ? "- Corte clásico" : String.join("\n", nombres);
    }

    private String generarVistaPrevia(RecomendacionCorte rec, GeminiClient.Imagen foto) {
        try {
            GeminiClient.Imagen editada = gemini.editarImagen(
                    prompts.previewImagen(rec.getCorteRecomendado(), rec.getPromptImagen()), foto);
            return guardar(editada, "previews");
        } catch (RuntimeException | IOException e) {
            // La recomendación en texto sigue siendo útil aunque falle la imagen
            log.warn("No se pudo generar la vista previa: {}", e.getMessage());
            return null;
        }
    }

    private void validar(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new IllegalArgumentException("Adjunta una foto de tu rostro");
        }
        if (foto.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("La foto no debe superar 5 MB");
        }
        if (foto.getContentType() == null || !TIPOS_PERMITIDOS.contains(foto.getContentType())) {
            throw new IllegalArgumentException("Formato no permitido. Usa JPG, PNG o WEBP");
        }
    }

    private String guardar(GeminiClient.Imagen imagen, String subcarpeta) throws IOException {
        String extension = switch (imagen.mimeType()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        Path carpeta = Paths.get(carpetaUploads, "ia", subcarpeta).toAbsolutePath();
        Files.createDirectories(carpeta);
        String nombre = UUID.randomUUID() + extension;
        Files.write(carpeta.resolve(nombre), imagen.datos());
        return "/uploads/ia/" + subcarpeta + "/" + nombre;
    }

    private RecomendacionCorte recomendacionDemo() {
        try {
            return objectMapper.readValue("""
                    {"rostroDetectado":true,"formaRostro":"OVALADO",
                     "rasgos":["frente amplia","mandíbula suave"],
                     "cortes":[
                       {"nombre":"Pompadour clásico","motivo":"Modo demostración: configura GEMINI_API_KEY para un análisis real.","mantenimientoSemanas":3},
                       {"nombre":"Undercut","motivo":"Laterales cortos que limpian el contorno.","mantenimientoSemanas":3},
                       {"nombre":"Crop texturizado","motivo":"Flequillo corto que suaviza la frente.","mantenimientoSemanas":4}],
                     "corteRecomendado":"Pompadour clásico","servicioSugerido":null,
                     "promptImagen":"classic pompadour, short tapered sides",
                     "mensaje":"(Demo) Te recomendamos un Pompadour clásico. Reserva en La Clásica con tu DNI."}
                    """, RecomendacionCorte.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
