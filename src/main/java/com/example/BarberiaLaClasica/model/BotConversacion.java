package com.example.BarberiaLaClasica.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

/** U4: una conversación del agente de citas, desde el primer mensaje hasta que se crea la cita o se abandona. */
@Entity
@Table(name = "bot_conversaciones", indexes = @Index(name = "idx_bot_conv_chat", columnList = "chat_id"))
@Data
public class BotConversacion {

    public static final String EN_CURSO = "EN_CURSO";
    public static final String CITA_CREADA = "CITA_CREADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, length = 60)
    private String chatId;

    /** TELEGRAM o PRUEBA (conversaciones del script de pruebas). */
    @Column(nullable = false, length = 15)
    private String canal;

    @Column(length = 8)
    private String dni;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(name = "ultima_actividad", nullable = false)
    private LocalDateTime ultimaActividad;

    private LocalDateTime fin;

    @Column(nullable = false, length = 15)
    private String resultado = EN_CURSO;

    @Column(name = "cita_id")
    private Long citaId;

    private int mensajes;

    @Column(name = "errores_controlados")
    private int erroresControlados;
}
