package com.example.BarberiaLaClasica.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

/**
 * U3 - Lectura enviada por un nodo IoT (sensor de presencia de una silla o
 * sensor de temperatura y humedad del local).
 */
@Entity
@Table(name = "lecturas_iot", indexes = {
        @Index(name = "idx_lectura_fecha", columnList = "fecha"),
        @Index(name = "idx_lectura_tipo_silla", columnList = "tipo, silla_id")
})
@Data
public class LecturaIot {

    public static final String SILLA = "SILLA";
    public static final String AMBIENTE = "AMBIENTE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SILLA | AMBIENTE
    @Column(nullable = false, length = 10)
    private String tipo;

    // Número de silla = id del barbero (igual que "SILLA #" en recepción)
    @Column(name = "silla_id")
    private Long sillaId;

    private Boolean ocupada;

    @Column(name = "distancia_cm")
    private Double distanciaCm;

    private Double temperatura;

    private Double humedad;

    @Column(length = 40)
    private String dispositivo;

    // MQTT | HTTP
    @Column(length = 5)
    private String canal;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
