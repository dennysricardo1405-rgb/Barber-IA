// ═══════════════════════════════════════════════════════════════════
// ARCHIVO 5: ConfiguracionSitioService.java
// ═══════════════════════════════════════════════════════════════════
package com.example.BarberiaLaClasica.service;
 
import com.example.BarberiaLaClasica.model.ConfiguracionSitio;
import com.example.BarberiaLaClasica.repository.ConfiguracionSitioRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
 
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
 
@Service
public class ConfiguracionSitioService {
 
    private final ConfiguracionSitioRepository repo;
    private static final String UPLOAD_DIR = "uploads/config/";
 
    // Claves con sus valores por defecto (para inicialización automática)
    private static final Map<String, String[]> DEFAULTS = new LinkedHashMap<>();
    static {
        // {tipo, grupo, etiqueta, valorDefault}
        DEFAULTS.put("nombre_barberia",  new String[]{"texto",  "identidad", "Nombre de la Barbería",      "La Clásica"});
        DEFAULTS.put("slogan",           new String[]{"texto",  "identidad", "Slogan principal",           "El Arte del Buen Corte"});
        DEFAULTS.put("hero_tag",         new String[]{"texto",  "identidad", "Etiqueta hero",              "Barbería de Élite"});
        DEFAULTS.put("hero_descripcion", new String[]{"texto",  "identidad", "Descripción hero",           "Reserva tu cita con los mejores barberos de la ciudad."});
        DEFAULTS.put("logo_url",         new String[]{"imagen", "identidad", "Logo",                       ""});
        DEFAULTS.put("color_primario",   new String[]{"color",  "identidad", "Color primario",             "#c9a84c"});
        DEFAULTS.put("telefono",         new String[]{"texto",  "contacto",  "Teléfono",                   "+51 951 578 601"});
        DEFAULTS.put("email",            new String[]{"texto",  "contacto",  "Email",                      ""});
        DEFAULTS.put("direccion",        new String[]{"texto",  "contacto",  "Dirección",                  "Av. los Incas 650, La Victoria"});
        DEFAULTS.put("ciudad",           new String[]{"texto",  "contacto",  "Ciudad",                     "Chiclayo, Lambayeque"});
        DEFAULTS.put("whatsapp_numero",  new String[]{"texto",  "contacto",  "WhatsApp número",            "51951578601"});
        DEFAULTS.put("horario_semana",   new String[]{"texto",  "horarios",  "Lunes – Viernes",            "10:00 - 21:00"});
        DEFAULTS.put("horario_sabado",   new String[]{"texto",  "horarios",  "Sábado",                     "08:00 - 22:00"});
        DEFAULTS.put("horario_domingo",  new String[]{"texto",  "horarios",  "Domingo",                    "10:00 - 18:00"});
        DEFAULTS.put("horario_especial", new String[]{"texto",  "horarios",  "Horario especial",           ""});
        DEFAULTS.put("footer_copyright", new String[]{"texto",  "contacto",  "Footer copyright",          "© 2026 BarberPro — La Clásica"});
        DEFAULTS.put("footer_descripcion",new String[]{"texto", "contacto",  "Descripción footer",        "Definiendo el estilo del caballero moderno en Chiclayo desde 2026."});
        DEFAULTS.put("facebook_url",     new String[]{"url",    "redes",     "Facebook",                   ""});
        DEFAULTS.put("instagram_url",    new String[]{"url",    "redes",     "Instagram",                  ""});
        DEFAULTS.put("whatsapp_url",     new String[]{"url",    "redes",     "WhatsApp URL",               ""});
        DEFAULTS.put("tiktok_url",       new String[]{"url",    "redes",     "TikTok",                     ""});
        DEFAULTS.put("youtube_url",      new String[]{"url",    "redes",     "YouTube",                    ""});
        DEFAULTS.put("yape_numero",      new String[]{"texto",  "yape", "Número de Yape",          "987 654 321"});
        DEFAULTS.put("yape_titular",     new String[]{"texto",  "yape", "Nombre del titular",       "Barbería La Clásica"});
        DEFAULTS.put("yape_porcentaje",  new String[]{"texto",  "yape", "% de adelanto requerido",  "25"});
        DEFAULTS.put("yape_qr_url",      new String[]{"imagen", "yape", "Imagen QR de Yape",        ""});
    }
 
    public ConfiguracionSitioService(ConfiguracionSitioRepository repo) {
        this.repo = repo;
    }
 
    /**
     * Devuelve un Map<clave, valor> con TODOS los campos de configuración.
     * Si no existe una clave en BD se devuelve el valor por defecto.
     * Usar en los controllers: model.addAttribute("config", configService.obtenerMapa());
     */
    public Map<String, String> obtenerMapa() {
        Map<String, String> mapa = new LinkedHashMap<>();
        // Poner defaults primero
        DEFAULTS.forEach((k, v) -> mapa.put(k, v[3]));
        // Sobrescribir con los valores guardados en BD
        repo.findAll().forEach(c -> mapa.put(c.getClave(), c.getValor() != null ? c.getValor() : ""));
        return mapa;
    }
 
    /**
     * Guarda/actualiza un grupo de claves a partir de un Map de parámetros del formulario.
     * @param params   Map<clave, valor> venido del formulario
     * @param logoFile archivo de imagen del logo (puede ser null)
     */
    
    public void guardarGrupo(Map<String, String> params, MultipartFile logoFile, MultipartFile yapeQrFile) throws IOException {
    // Logo
    if (logoFile != null && !logoFile.isEmpty()) {
        String ext = "";
        String orig = logoFile.getOriginalFilename();
        if (orig != null && orig.contains(".")) ext = orig.substring(orig.lastIndexOf("."));
        String nombre = "logo-" + UUID.randomUUID() + ext;
        Path destino = Paths.get(UPLOAD_DIR + nombre);
        Files.createDirectories(destino.getParent());
        Files.copy(logoFile.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        params.put("logo_url", "/uploads/config/" + nombre);
    }

    // QR de Yape
    if (yapeQrFile != null && !yapeQrFile.isEmpty()) {
        String ext = "";
        String orig = yapeQrFile.getOriginalFilename();
        if (orig != null && orig.contains(".")) ext = orig.substring(orig.lastIndexOf("."));
        String nombre = "yape-qr-" + UUID.randomUUID() + ext;
        Path destino = Paths.get(UPLOAD_DIR + nombre);
        Files.createDirectories(destino.getParent());
        Files.copy(yapeQrFile.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        params.put("yape_qr_url", "/uploads/config/" + nombre);
    }

    for (Map.Entry<String, String> entry : params.entrySet()) {
        String clave = entry.getKey();
        if (!DEFAULTS.containsKey(clave)) continue;
        String[] meta = DEFAULTS.get(clave);
        ConfiguracionSitio cfg = repo.findById(clave)
            .orElse(new ConfiguracionSitio(clave, "", meta[0], meta[1], meta[2]));
        cfg.setValor(entry.getValue());
        repo.save(cfg);
    }
}
 
    /**
     * Inicializa la BD con todos los defaults si están vacíos.
     * Llámalo en un @PostConstruct o CommandLineRunner.
     */
    public void inicializarDefaults() {
        DEFAULTS.forEach((clave, meta) -> {
            if (!repo.existsById(clave)) {
                repo.save(new ConfiguracionSitio(clave, meta[3], meta[0], meta[1], meta[2]));
            }
        });
    }
}
 
