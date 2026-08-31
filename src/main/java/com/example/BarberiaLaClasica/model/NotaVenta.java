package com.example.BarberiaLaClasica.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Entity
@Table(name = "notas_venta")
@Data
public class NotaVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private SillaSession session;

    @CreationTimestamp
    @Column(name = "fecha", updatable = false)
    private LocalDateTime fecha;

    private double total;

    @OneToMany(mappedBy = "notaVenta", cascade = CascadeType.ALL)
    private List<DetalleNotaVenta> detalles = new ArrayList<>();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barbero_id")
    private Barbero barbero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
    @Column(name = "metodo_pago", length = 20)
    private String metodoPago = "EFECTIVO";
 
    @Column(name = "monto_yape")
    private double montoYape = 0.0;
 
    @Column(name = "monto_efectivo")
    private double montoEfectivo = 0.0;
 
    @Column(name = "codigo_yape", length = 20)
    private String codigoYape;
    public java.time.LocalDateTime getFecha() {
    return this.fecha;
}
}
