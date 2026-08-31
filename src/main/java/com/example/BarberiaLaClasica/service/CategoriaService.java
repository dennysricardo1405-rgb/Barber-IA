package com.example.BarberiaLaClasica.service;

import com.example.BarberiaLaClasica.model.Categoria;
import com.example.BarberiaLaClasica.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll();
    }

    public org.springframework.data.domain.Page<Categoria> listarTodasPaginadas(org.springframework.data.domain.Pageable pageable) {
        return categoriaRepository.findAll(pageable);
    }

    public List<Categoria> listarPrincipalesActivas() {
        return categoriaRepository.findByPadreIsNullAndActivoTrue();
    }

    public List<Categoria> listarSubcategoriasPorPadre(Long padreId) {
        return categoriaRepository.findByPadreIdAndActivoTrue(padreId);
    }

    public List<Categoria> listarTodasActivas() {
        return categoriaRepository.findByActivoTrue();
    }

    @Transactional
    public Categoria guardar(Categoria categoria) {
        // ✅ Validar nombre duplicado
        boolean existe = categoriaRepository.findAll().stream()
                .anyMatch(c -> c.getNombre().trim().equalsIgnoreCase(categoria.getNombre().trim())
                        && !c.getId().equals(categoria.getId())); // Permite editar sin error

        if (existe) {
            throw new RuntimeException("Ya existe una categoría con el nombre: " + categoria.getNombre());
        }

        return categoriaRepository.save(categoria);
    }

    public Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría o Subcategoría no encontrada con ID: " + id));
    }

    @Transactional
    public void cambiarEstado(Long id) {
        Categoria categoria = buscarPorId(id);
        categoria.setActivo(!categoria.isActivo());
        categoriaRepository.save(categoria);
    }
}