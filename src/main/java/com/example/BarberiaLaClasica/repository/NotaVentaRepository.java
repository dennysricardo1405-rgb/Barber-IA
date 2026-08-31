package com.example.BarberiaLaClasica.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.BarberiaLaClasica.model.Barbero;
import com.example.BarberiaLaClasica.model.NotaVenta;

@Repository
public interface NotaVentaRepository extends JpaRepository<NotaVenta, Long> {

    // 1. Para la Paginación de Notas de Venta en Recepción
    Page<NotaVenta> findAll(Pageable pageable);

    // 2. Para el filtro de Sueldos por Comisión (Usa funciones nativas de año y mes de MySQL)
    @Query("SELECT n FROM NotaVenta n WHERE n.barbero = :barbero AND YEAR(n.fecha) = :anio AND MONTH(n.fecha) = :mes")
    List<NotaVenta> findByBarberoAndPeriodo(
            @Param("barbero") Barbero barbero, 
            @Param("anio") int anio, 
            @Param("mes") int mes);

    // 3. Contar compras/visitas realizadas por un cliente
    long countByCliente(com.example.BarberiaLaClasica.model.Cliente cliente);

    // 4. Buscar notas de venta por rango de fechas
    List<NotaVenta> findByFechaBetween(java.time.LocalDateTime inicio, java.time.LocalDateTime fin);

    // 5. Buscar notas paginadas por rango de fechas
    Page<NotaVenta> findByFechaBetween(java.time.LocalDateTime inicio, java.time.LocalDateTime fin, Pageable pageable);

    // 6. Buscar notas de venta filtradas por origen (CAJA vs SILLA) y texto (Cliente/Barbero/Producto/Servicio) con paginación
    @Query("SELECT DISTINCT n FROM NotaVenta n " +
           "LEFT JOIN n.cliente c " +
           "LEFT JOIN n.barbero b " +
           "LEFT JOIN n.detalles d " +
           "WHERE n.fecha BETWEEN :inicio AND :fin " +
           "AND (:origen IS NULL OR :origen = '' OR " +
           "     (:origen = 'CAJA' AND n.barbero IS NULL) OR " +
           "     (:origen = 'SILLA' AND n.barbero IS NOT NULL)) " +
           "AND (:search IS NULL OR :search = '' OR " +
           "     LOWER(c.nombres) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(c.apellidos) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(b.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(d.descripcion) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY n.fecha DESC")
    Page<NotaVenta> buscarNotasVentaFiltradas(
            @Param("inicio") java.time.LocalDateTime inicio,
            @Param("fin") java.time.LocalDateTime fin,
            @Param("origen") String origen,
            @Param("search") String search,
            Pageable pageable);
}