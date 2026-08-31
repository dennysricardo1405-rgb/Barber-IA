package com.example.BarberiaLaClasica.service;

import com.example.BarberiaLaClasica.model.CompraProveedor;
import com.example.BarberiaLaClasica.model.Producto;
import com.example.BarberiaLaClasica.model.Proveedor;
import com.example.BarberiaLaClasica.repository.ProductoRepository;
import com.example.BarberiaLaClasica.repository.ProveedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProveedorService {

    @Autowired
    private ProveedorRepository proveedorRepository;
    @Autowired
    private ProductoRepository productoRepository;

    public List<Proveedor> listarTodos() {
        return proveedorRepository.findAll();
    }

    public List<Proveedor> listarActivos() {
        return proveedorRepository.findByActivoTrue();
    }

    public Proveedor buscarPorId(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + id));
    }

    @Transactional
    public Proveedor guardar(Proveedor proveedor) {
        return proveedorRepository.save(proveedor);
    }

    @Transactional
    public void cambiarEstado(Long id) {
        Proveedor proveedor = buscarPorId(id);
        proveedor.setActivo(!proveedor.isActivo());
        proveedorRepository.save(proveedor);
    }

    @Transactional
    public void registrarCompra(CompraProveedor compra) {
        // Buscamos el producto real en la base de datos
        Producto producto = productoRepository.findById(compra.getProducto().getId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        int stockAIncrementar = 0;

        if ("PAQUETE".equals(compra.getTipoCompra())) {
            // Escenario 1: Paquetes * Unidades por paquete
            stockAIncrementar = compra.getCantidadPaquetes() * compra.getUnidadesPorPaquete();
        } else {
            // Escenario 2: Unidades sueltas directas
            stockAIncrementar = compra.getCantidadPaquetes(); // En el HTML usamos el mismo campo para cantidad suelta
        }

        // Incrementamos el stock actual del producto
        producto.setStock(producto.getStock() + stockAIncrementar);

        // Actualizamos el precio de venta al público con el nuevo valor fijado en la
        // compra
        producto.setPrecioVenta(compra.getPrecioVentaUnidad());

        // Guardamos los cambios del producto
        productoRepository.save(producto);

        // Aquí puedes guardar el registro de la compra en su propia tabla si usas un
        // CompraProveedorRepository
        // compraProveedorRepository.save(compra);
    }
}