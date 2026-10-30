package com.example.BarberiaLaClasica.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Catálogo de cortes con foto de referencia. El asesor IA muestra esta foto
 * para que el cliente vea cómo luce el corte que se le recomienda.
 */
@Entity
@Table(name = "cortes_referencia")
@Data
public class CorteReferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    private String descripcion;

    // ruta /uploads/cortes/archivo.jpg
    private String imagen;

    private boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;
}
