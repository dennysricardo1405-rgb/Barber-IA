package com.example.BarberiaLaClasica.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "promociones")
@Data
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    // "SERVICIO" o "PRODUCTO"
    @Column(name = "tipo_promocion", nullable = false)
    private String tipoPromocion; 

    @Column(name = "porcentaje_descuento", nullable = false)
    private BigDecimal porcentajeDescuento;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    // Regla de aplicación por visitas (0 si aplica a todos)
    @Column(name = "minimo_visitas_requeridas")
    private int minimoVisitasRequeridas = 0;

    private boolean activo = true;

    // --- RELACIONES OPCIONALES ---
    // Si aplica a un servicio en específico
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

    // Si aplica a un producto en específico
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    // Si aplica a toda una categoría de productos
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    /**
     * Método auxiliar para verificar en tiempo real si la promoción sigue vigente.
     * Esto nos ayudará con la desactivación automática.
     */
    public boolean isVigente() {
        LocalDateTime ahora = LocalDateTime.now();
        return activo && !ahora.isBefore(fechaInicio) && !ahora.isAfter(fechaFin);
    }

    /**
     * Devuelve el precio final calculado con descuento si aplica a un servicio o producto específico.
     */
    public Double getPrecioFinalEstimado() {
        if (porcentajeDescuento == null) return null;
        double desc = porcentajeDescuento.doubleValue();
        if (servicio != null && servicio.getPrecio() != null) {
            double orig = servicio.getPrecio().doubleValue();
            return BigDecimal.valueOf(orig - (orig * (desc / 100.0))).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
        if (producto != null) {
            double orig = producto.getPrecioVenta();
            return BigDecimal.valueOf(orig - (orig * (desc / 100.0))).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
        return null;
    }
}