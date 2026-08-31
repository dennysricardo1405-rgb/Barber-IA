package com.example.BarberiaLaClasica.repository;

import com.example.BarberiaLaClasica.model.HistorialInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface HistorialInventarioRepository extends JpaRepository<HistorialInventario, Long> {
    
    // Búsqueda paginada con filtro de rango de fechas completo
    Page<HistorialInventario> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin, Pageable pageable);

    // Búsqueda paginada general por si no hay fechas seleccionadas
    Page<HistorialInventario> findAll(Pageable pageable);
}