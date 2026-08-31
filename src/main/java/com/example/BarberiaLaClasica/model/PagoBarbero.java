package com.example.BarberiaLaClasica.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos_barberos")
@Data
public class PagoBarbero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barbero_id", nullable = false)
    private Barbero barbero;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montoPagado;

    @Column(nullable = false)
    private LocalDateTime fechaPago = LocalDateTime.now();

    @Column(nullable = false)
    private String periodoConsultado; // Ej: "2026-07"

    @Column(nullable = false)
    private String tipoPago; // "ADELANTO" o "LIQUIDACION_TOTAL"

    @Column(nullable = true)
    private String descripcion;
}