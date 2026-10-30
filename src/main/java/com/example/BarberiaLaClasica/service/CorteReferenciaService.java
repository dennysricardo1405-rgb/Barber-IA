package com.example.BarberiaLaClasica.service;

import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.BarberiaLaClasica.model.CorteReferencia;
import com.example.BarberiaLaClasica.repository.CorteReferenciaRepository;

@Service
public class CorteReferenciaService {

    private final CorteReferenciaRepository repo;
    private final AlmacenImagenes almacen;

    public CorteReferenciaService(CorteReferenciaRepository repo, AlmacenImagenes almacen) {
        this.repo = repo;
        this.almacen = almacen;
    }

    public List<CorteReferencia> listarTodos() {
        return repo.findAllByOrderByNombreAsc();
    }

    public CorteReferencia guardar(Long id, String nombre, String descripcion, MultipartFile foto) throws IOException {
        nombre = nombre == null ? "" : nombre.trim();
        if (nombre.length() < 2 || nombre.length() > 80) {
            throw new IllegalArgumentException("El nombre debe tener entre 2 y 80 caracteres.");
        }
        boolean repetido = id == null ? repo.existsByNombreIgnoreCase(nombre)
                : repo.existsByNombreIgnoreCaseAndIdNot(nombre, id);
        if (repetido) {
            throw new IllegalArgumentException("Ya existe un corte con ese nombre.");
        }
        CorteReferencia corte = id == null ? new CorteReferencia()
                : repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Corte no encontrado"));
        boolean hayFoto = foto != null && !foto.isEmpty();
        if (!hayFoto && corte.getImagen() == null) {
            throw new IllegalArgumentException("Selecciona una foto del corte.");
        }
        corte.setNombre(nombre);
        corte.setDescripcion(descripcion == null || descripcion.isBlank() ? null : descripcion.trim());
        if (hayFoto) {
            String anterior = corte.getImagen();
            corte.setImagen(almacen.guardar(foto, "cortes"));
            almacen.eliminar(anterior);
        }
        return repo.save(corte);
    }

    public void cambiarEstado(Long id) {
        repo.findById(id).ifPresent(c -> {
            c.setActivo(!c.isActivo());
            repo.save(c);
        });
    }

    public void eliminar(Long id) {
        repo.findById(id).ifPresent(c -> {
            almacen.eliminar(c.getImagen());
            repo.delete(c);
        });
    }

    /** Lista de nombres para el prompt, así la IA recomienda cortes que tienen foto. */
    public String nombresParaPrompt() {
        return repo.findByActivoTrueOrderByNombreAsc().stream()
                .map(c -> "- " + c.getNombre())
                .collect(Collectors.joining("\n"));
    }

    /** Foto del corte que más se parece al nombre que devolvió la IA, o null si no hay. */
    public String fotoDe(String nombreCorte) {
        return fotoDe(nombreCorte, repo.findByActivoTrueOrderByNombreAsc());
    }

    static String fotoDe(String nombreCorte, List<CorteReferencia> catalogo) {
        String buscado = normalizar(nombreCorte);
        if (buscado.isEmpty()) return null;
        CorteReferencia parecido = null;
        for (CorteReferencia c : catalogo) {
            String nombre = normalizar(c.getNombre());
            if (nombre.isEmpty() || c.getImagen() == null) continue;
            if (nombre.equals(buscado)) return c.getImagen();
            // "Low fade con textura" debe encontrar "Low fade"; se prefiere el nombre más largo
            if ((contienePalabras(buscado, nombre) || contienePalabras(nombre, buscado))
                    && (parecido == null || nombre.length() > normalizar(parecido.getNombre()).length())) {
                parecido = c;
            }
        }
        return parecido == null ? null : parecido.getImagen();
    }

    private static boolean contienePalabras(String texto, String parte) {
        return (" " + texto + " ").contains(" " + parte + " ");
    }

    static String normalizar(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
    }
}
