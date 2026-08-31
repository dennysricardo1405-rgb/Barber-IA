package com.example.BarberiaLaClasica.model;

import jakarta.persistence.*;
import lombok.Data;

import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "barberos")
@Data
public class Barbero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String especialidad;
    private String telefono;

    @Column(name = "dia_libre", nullable = false)
    private String diaLibre = "MARTES";

    private int estado = 1;

    private String imagen;   // ← único campo nuevo: ruta /uploads/barberos/archivo.jpg

    @CreationTimestamp
    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;

    // Constructor vacío
    public Barbero() {}

}