package com.example.BarberiaLaClasica.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_inventario")
@Data
public class HistorialInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "tipo_movimiento", nullable = false, length = 20)
    private String tipoMovimiento; // "ENTRADA" o "SALIDA"

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "stock_resultante", nullable = false)
    private int stockResultante;

    @Column(nullable = false)
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();
}