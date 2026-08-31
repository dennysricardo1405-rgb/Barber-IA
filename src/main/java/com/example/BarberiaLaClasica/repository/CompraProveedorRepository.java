package com.example.BarberiaLaClasica.repository;

import com.example.BarberiaLaClasica.model.CompraProveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CompraProveedorRepository extends JpaRepository<CompraProveedor, Long> {
    
    // Método para la paginación que ya tenías corriendo fino
    Page<CompraProveedor> findAllByOrderByIdDesc(Pageable pageable);

    // ── ESTA ES LA ÚNICA CONSULTA POR RANGOS DE FECHA QUE DEBE QUEDAR ──
    List<CompraProveedor> findByFechaCompraBetween(LocalDateTime inicio, LocalDateTime fin);
}