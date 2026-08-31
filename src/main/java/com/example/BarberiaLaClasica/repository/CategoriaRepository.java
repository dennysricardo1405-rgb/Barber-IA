package com.example.BarberiaLaClasica.repository;

import com.example.BarberiaLaClasica.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    
    // Devuelve solo las categorías principales (Bebidas, Snacks, etc.) que estén activas
    List<Categoria> findByPadreIsNullAndActivoTrue();

    // Devuelve todas las subcategorías de un padre específico (Ej: todas las de "Bebidas")
    List<Categoria> findByPadreIdAndActivoTrue(Long padreId);
    
    // Devuelve absolutamente todas las categorías que estén activas (útil para asignar a un producto)
    List<Categoria> findByActivoTrue();
    List<Categoria> findByPadreNombreAndActivoTrue(String nombrePadre);
    List<Categoria> findByPadreIsNotNullAndActivoTrue();
}