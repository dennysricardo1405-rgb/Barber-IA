package com.example.BarberiaLaClasica.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "compras_proveedor")
@Data
public class CompraProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = true) 
    private Proveedor proveedor;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    @Column(name = "es_compra_directa", nullable = false)
    private boolean esCompraDirecta = false;
    @Enumerated(EnumType.STRING)
    private TipoCompra tipoCompra;
    private Integer cantidadPaquetes;
    private Integer unidadesPorPaquete;
    private int totalUnidades;
    private double precioCompraPaquete;
    private double precioVentaUnidad;
    private double totalInvertido;
    private LocalDateTime fechaCompra;

    @PrePersist
    protected void onCreate() {
        this.fechaCompra = LocalDateTime.now();
    }

    public enum TipoCompra {
        PAQUETE, UNIDAD
    }

    public Proveedor getProveedor() {
        return this.proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public Producto getProducto() {
        return this.producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }
}