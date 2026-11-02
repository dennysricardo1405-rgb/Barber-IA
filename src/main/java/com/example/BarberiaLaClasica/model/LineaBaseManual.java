package com.example.BarberiaLaClasica.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;

/** U4: un agendamiento manual cronometrado (antes del bot), para comparar la eficiencia. */
@Entity
@Table(name = "linea_base_manual")
@Data
public class LineaBaseManual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    /** Minutos que el personal estuvo ocupado atendiendo el pedido. */
    @Column(name = "minutos_atencion", nullable = false)
    private double minutosAtencion;

    /** Minutos desde el primer mensaje del cliente hasta que tuvo su cita confirmada. */
    @Column(name = "minutos_total", nullable = false)
    private double minutosTotal;

    @Column(length = 200)
    private String nota;
}
