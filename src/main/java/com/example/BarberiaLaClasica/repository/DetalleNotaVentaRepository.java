package com.example.BarberiaLaClasica.repository;

import com.example.BarberiaLaClasica.model.DetalleNotaVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DetalleNotaVentaRepository extends JpaRepository<DetalleNotaVenta, Long> {

    // Usado en la pestaña "Productos" — filtra solo por tipo (PRODUCTO o SERVICIO)
    @Query("SELECT d FROM DetalleNotaVenta d " +
           "WHERE d.tipo = :tipo " +
           "AND d.notaVenta.fecha BETWEEN :inicio AND :fin")
    List<DetalleNotaVenta> findByTipoAndFechaBetween(
            @Param("tipo") String tipo,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    // Usado en la pestaña "Todos" — trae todo sin importar el tipo
    @Query("SELECT d FROM DetalleNotaVenta d WHERE d.notaVenta.fecha BETWEEN :inicio AND :fin")
    List<DetalleNotaVenta> findByFechaBetween(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    // Paginación exclusiva para ventas de productos (Venta Directa en Caja + Consumo en Silla por Barbero)
    @Query("SELECT d FROM DetalleNotaVenta d WHERE UPPER(d.tipo) = 'PRODUCTO' AND d.notaVenta.fecha BETWEEN :inicio AND :fin ORDER BY d.notaVenta.fecha DESC")
    org.springframework.data.domain.Page<DetalleNotaVenta> findVentasProductosPaginadas(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            org.springframework.data.domain.Pageable pageable);

    // Paginación con filtros de Origen (CAJA vs SILLA) y Búsqueda por Producto / Cliente
    @Query("SELECT d FROM DetalleNotaVenta d " +
           "WHERE UPPER(d.tipo) = 'PRODUCTO' " +
           "AND d.notaVenta.fecha BETWEEN :inicio AND :fin " +
           "AND (:origen IS NULL OR :origen = '' OR " +
           "     (:origen = 'CAJA' AND d.notaVenta.barbero IS NULL) OR " +
           "     (:origen = 'SILLA' AND d.notaVenta.barbero IS NOT NULL)) " +
           "AND (:search IS NULL OR :search = '' OR " +
           "     LOWER(d.descripcion) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     (d.notaVenta.cliente IS NOT NULL AND (LOWER(d.notaVenta.cliente.nombres) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(d.notaVenta.cliente.apellidos) LIKE LOWER(CONCAT('%', :search, '%'))))) " +
           "ORDER BY d.notaVenta.fecha DESC")
    org.springframework.data.domain.Page<DetalleNotaVenta> buscarVentasProductosFiltradas(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("origen") String origen,
            @Param("search") String search,
            org.springframework.data.domain.Pageable pageable);
}