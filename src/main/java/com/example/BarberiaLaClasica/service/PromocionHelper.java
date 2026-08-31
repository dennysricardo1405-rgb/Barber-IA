package com.example.BarberiaLaClasica.service;

import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.model.Producto;
import com.example.BarberiaLaClasica.model.Promocion;
import com.example.BarberiaLaClasica.model.Servicio;
import com.example.BarberiaLaClasica.repository.CitaRepository;
import com.example.BarberiaLaClasica.repository.PromocionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class PromocionHelper {

    private final PromocionRepository promocionRepository;
    private final CitaRepository citaRepository;

    @Autowired
    public PromocionHelper(PromocionRepository promocionRepository, CitaRepository citaRepository) {
        this.promocionRepository = promocionRepository;
        this.citaRepository = citaRepository;
    }

    /**
     * Sobrecarga general sin cliente (evalúa promos sin restricción de visitas ni uso previo)
     */
    public double calcularPrecioServicio(Servicio servicio) {
        return calcularPrecioServicio(servicio, null);
    }

    /**
     * Calcula el precio de un servicio evaluando visitas del cliente y uso único de la promo.
     */
    public double calcularPrecioServicio(Servicio servicio, Cliente cliente) {
        if (servicio == null) return 0.0;
        double precioOriginal = servicio.getPrecio().doubleValue();
        List<Promocion> vigentes = promocionRepository.findPromocionesVigentes(LocalDateTime.now());

        double maxDescuento = 0.0;

        for (Promocion p : vigentes) {
            if ("SERVICIO".equalsIgnoreCase(p.getTipoPromocion()) && p.getServicio() != null) {
                if (p.getServicio().getId().equals(servicio.getId())) {

                    // 1. Validar fidelización por mínimo de visitas requeridas
                    if (p.getMinimoVisitasRequeridas() > 0) {
                        if (cliente == null || cliente.getTotalVisitas() < p.getMinimoVisitasRequeridas()) {
                            continue; // El cliente no cumple las visitas requeridas
                        }
                    }

                    double desc = p.getPorcentajeDescuento().doubleValue();
                    if (desc > maxDescuento) {
                        maxDescuento = desc;
                    }
                }
            }
        }

        if (maxDescuento > 0) {
            double descuento = precioOriginal * (maxDescuento / 100.0);
            return BigDecimal.valueOf(precioOriginal - descuento)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        return precioOriginal;
    }

    /**
     * Sobrecarga general de productos sin cliente
     */
    public double calcularPrecioProducto(Producto producto) {
        return calcularPrecioProducto(producto, null);
    }

    /**
     * Calcula el precio de un producto aplicando la promoción por ID o por Categoría evaluando visitas.
     */
    public double calcularPrecioProducto(Producto producto, Cliente cliente) {
        if (producto == null) return 0.0;
        double precioOriginal = producto.getPrecioVenta();
        List<Promocion> vigentes = promocionRepository.findPromocionesVigentes(LocalDateTime.now());

        double maxDescuento = 0.0;

        for (Promocion p : vigentes) {
            if ("PRODUCTO".equalsIgnoreCase(p.getTipoPromocion())) {
                // Caso 1: Aplica al producto específico
                if (p.getProducto() != null && p.getProducto().getId().equals(producto.getId())) {
                    double desc = p.getPorcentajeDescuento().doubleValue();
                    if (desc > maxDescuento) maxDescuento = desc;
                }
                // Caso 2: Aplica a toda la categoría del producto
                else if (p.getCategoria() != null && producto.getCategoria() != null 
                        && p.getCategoria().getId().equals(producto.getCategoria().getId())) {
                    double desc = p.getPorcentajeDescuento().doubleValue();
                    if (desc > maxDescuento) maxDescuento = desc;
                }
            }
        }

        if (maxDescuento > 0) {
            double descuento = precioOriginal * (maxDescuento / 100.0);
            return BigDecimal.valueOf(precioOriginal - descuento)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        return precioOriginal;
    }
}