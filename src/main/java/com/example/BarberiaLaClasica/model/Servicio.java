package com.example.BarberiaLaClasica.model;

import jakarta.persistence.*;
import lombok.Data;

import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "servicios")
@Data
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(nullable = false)
    private BigDecimal precio;

    @Column(name = "duracion_minutos", nullable = false)
    private int duracionMinutos = 30;

    private int estado = 1;

    @CreationTimestamp
    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;
    
    @Column(name = "imagen_url")
    private String imagenUrl;

    public Servicio() {
    }
}
