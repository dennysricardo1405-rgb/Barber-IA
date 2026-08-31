package com.example.BarberiaLaClasica.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "gastos_local")
@Data
public class GastoLocal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private double monto;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();
}