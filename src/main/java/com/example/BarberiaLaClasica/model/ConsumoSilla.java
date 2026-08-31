package com.example.BarberiaLaClasica.model;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "consumos_silla")
@Data
public class ConsumoSilla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private SillaSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = true)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = true)
    private Servicio servicio;

    @Column(name = "tipo", length = 20)
    private String tipo = "PRODUCTO";

    private int cantidad = 1;
    private double subtotal;
}
