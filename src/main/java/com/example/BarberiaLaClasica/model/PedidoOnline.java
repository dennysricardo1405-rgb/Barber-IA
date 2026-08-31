package com.example.BarberiaLaClasica.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
@Entity
@Table(name = "pedidos_online")
@Data
public class PedidoOnline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // 1=PENDIENTE, 2=ACEPTADO, 0=RECHAZADO
    @Column(nullable = false)
    private int estado = 1;

    @Column(name = "comprobante_pago")
    private String comprobantePago;

    @Column(name = "total", nullable = false)
    private double total;

    @Column(name = "fecha_pedido")
    private LocalDateTime fechaPedido;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DetallePedido> detalles = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.fechaPedido = LocalDateTime.now();
    }
}
