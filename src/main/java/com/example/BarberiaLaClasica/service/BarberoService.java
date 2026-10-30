package com.example.BarberiaLaClasica.service;

import com.example.BarberiaLaClasica.model.Barbero;
import com.example.BarberiaLaClasica.model.Usuario;
import com.example.BarberiaLaClasica.repository.BarberoRepository;
import com.example.BarberiaLaClasica.repository.ClienteRepository;
import com.example.BarberiaLaClasica.repository.PerfilRepository;
import com.example.BarberiaLaClasica.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BarberoService {

    @Autowired
    private BarberoRepository barberoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private PerfilRepository perfilRepository;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/barberos/";

    // ── Métodos existentes ─────────────────────────────────────
    public List<Barbero> listarTodos() {
        return barberoRepository.findAll();
    }

    public List<Barbero> listarActivos() {
        return barberoRepository.findByEstado(1);
    }

    public Optional<Barbero> buscarPorId(Long id) {
        return barberoRepository.findById(id);
    }

    public Barbero guardar(Barbero barbero, MultipartFile foto) throws IOException {
        if (foto != null && !foto.isEmpty()) {
            barbero.setImagen(guardarFoto(foto));
        }
        return barberoRepository.save(barbero);
    }

    public Barbero actualizar(Long id, Barbero datos, MultipartFile foto) throws IOException {
        Barbero barbero = barberoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Barbero no encontrado"));

        barbero.setNombre(datos.getNombre());
        barbero.setEspecialidad(datos.getEspecialidad());
        barbero.setTelefono(datos.getTelefono());
        barbero.setDiaLibre(datos.getDiaLibre());
        barbero.setEstado(datos.getEstado());

        if (foto != null && !foto.isEmpty()) {
            eliminarFotoAnterior(barbero.getImagen());
            barbero.setImagen(guardarFoto(foto));
        }
        return barberoRepository.save(barbero);
    }

    public void cambiarEstado(Long id) {
        Barbero barbero = barberoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Barbero no encontrado"));
        barbero.setEstado(barbero.getEstado() == 1 ? 0 : 1);
        // un barbero inactivo tampoco puede entrar a su panel
        if (barbero.getUsuario() != null) {
            barbero.getUsuario().setEstado(barbero.getEstado() == 0 ? 0 : 1);
            usuarioRepository.save(barbero.getUsuario());
        }
        barberoRepository.save(barbero);
    }

    public Optional<Barbero> buscarPorCorreo(String email) {
        return barberoRepository.findByUsuarioEmail(email);
    }

    /**
     * Crea o actualiza la cuenta con la que el barbero entra a su panel.
     * Si no se escribe correo no se toca nada; la contraseña solo es obligatoria al crearla.
     */
    public void asignarCuenta(Barbero barbero, String correo, String password) {
        correo = correo == null ? "" : correo.trim().toLowerCase();
        password = password == null ? "" : password;
        if (correo.isEmpty()) return;
        if (!correo.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            throw new IllegalArgumentException("El correo del barbero no es válido.");
        }
        Usuario usuario = barbero.getUsuario();
        boolean correoCambia = usuario == null || !correo.equalsIgnoreCase(usuario.getEmail());
        if (correoCambia && (usuarioRepository.existsByEmail(correo) || clienteRepository.findByCorreo(correo).isPresent())) {
            throw new IllegalArgumentException("Ese correo ya está en uso por otra cuenta.");
        }
        if (!password.isEmpty() && password.length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.");
        }
        if (usuario == null) {
            if (password.isEmpty()) {
                throw new IllegalArgumentException("Escribe una contraseña para la cuenta del barbero.");
            }
            usuario = new Usuario();
            usuario.setPerfil(perfilRepository.findByNombrePerfil("Barbero")
                    .orElseThrow(() -> new IllegalStateException("No existe el perfil Barbero")));
        }
        usuario.setNombre(barbero.getNombre());
        usuario.setEmail(correo);
        usuario.setEstado(barbero.getEstado() == 0 ? 0 : 1);
        if (!password.isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(password));
        }
        barbero.setUsuario(usuarioRepository.save(usuario));
        barberoRepository.save(barbero);
    }

    // ── NUEVOS MÉTODOS PARA PAGINACIÓN ─────────────────────────
    public Page<Barbero> listarPaginado(Pageable pageable) {
        return barberoRepository.findAll(pageable);
    }

    public Page<Barbero> buscar(String search, Pageable pageable) {
        if (search == null || search.trim().isEmpty()) {
            return listarPaginado(pageable);
        }
        String term = search.trim();
        return barberoRepository
                .findByNombreContainingIgnoreCaseOrEspecialidadContainingIgnoreCaseOrTelefonoContainingIgnoreCase(
                        term, term, term, pageable);
    }

    // ── Métodos privados ─────────────────────────────────────
    private String guardarFoto(MultipartFile foto) throws IOException {
        Files.createDirectories(Paths.get(UPLOAD_DIR));
        String ext = obtenerExtension(foto.getOriginalFilename());
        String nombreArchivo = UUID.randomUUID() + ext;
        Files.copy(foto.getInputStream(),
                   Paths.get(UPLOAD_DIR + nombreArchivo),
                   StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/barberos/" + nombreArchivo;
    }

    private void eliminarFotoAnterior(String imagen) {
        if (imagen == null) return;
        try {
            Files.deleteIfExists(Paths.get(System.getProperty("user.dir") + imagen));
        } catch (IOException ignored) {}
    }

    private String obtenerExtension(String nombre) {
        if (nombre != null && nombre.contains("."))
            return nombre.substring(nombre.lastIndexOf(".")).toLowerCase();
        return ".jpg";
    }
}