package com.example.BarberiaLaClasica.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

/** U4: bitácora de excepciones del agente (controladas por el backend o fallos reportados por n8n). */
@Entity
@Table(name = "bot_eventos")
@Data
public class BotEvento {

    public static final String ERROR_CONTROLADO = "ERROR_CONTROLADO";
    public static final String FALLO = "FALLO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversacion_id")
    private Long conversacionId;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(length = 500)
    private String detalle;
}
