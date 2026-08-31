package com.example.BarberiaLaClasica.model;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "silla_sessions")
@Data
public class SillaSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barbero_id", nullable = false)
    private Barbero barbero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente; // null = público general

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    // Si viene de una reserva previa, se vincula aquí
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    private Cita cita;

    @CreationTimestamp
    @Column(name = "inicio", updatable = false)
    private LocalDateTime inicio;

    private int estado = 1; // 1=activa, 0=cerrada
}