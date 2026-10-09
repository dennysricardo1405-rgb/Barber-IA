package com.example.BarberiaLaClasica.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Data;

/**
 * U2 - Resultado del asesor de imagen por IA (Gemini).
 * Guarda la foto original, la vista previa generada y la respuesta del modelo
 * para que el barbero vea el corte aprobado por el cliente.
 */
@Entity
@Table(name = "recomendaciones_ia")
@Data
public class RecomendacionIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente; // null hasta que el cliente apruebe con su DNI

    @Column(length = 8)
    private String dni;

    // ZERO_SHOT | ONE_SHOT | FEW_SHOT
    @Column(nullable = false, length = 20)
    private String estrategia;

    private String modelo;

    @Column(name = "forma_rostro", length = 20)
    private String formaRostro;

    @Column(name = "corte_recomendado")
    private String corteRecomendado;

    @Column(name = "servicio_sugerido")
    private String servicioSugerido;

    @Column(name = "mantenimiento_semanas")
    private Integer mantenimientoSemanas;

    @JsonIgnore
    @Column(name = "resultado_json", columnDefinition = "TEXT")
    private String resultadoJson;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Column(name = "preview_url")
    private String previewUrl;

    private boolean aprobada = false;

    // true cuando se generó sin API key (modo demostración)
    private boolean demo = false;

    @Column(name = "latencia_ms")
    private Long latenciaMs;

    @CreationTimestamp
    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;
}
