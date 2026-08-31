package com.example.BarberiaLaClasica.service;

import com.example.BarberiaLaClasica.model.CompraProveedor;
import com.example.BarberiaLaClasica.model.HistorialInventario;
import com.example.BarberiaLaClasica.model.Producto;
import com.example.BarberiaLaClasica.repository.CompraProveedorRepository;
import com.example.BarberiaLaClasica.repository.HistorialInventarioRepository;
import com.example.BarberiaLaClasica.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CompraProveedorRepository compraProveedorRepository;

    @Autowired
    private HistorialInventarioRepository historialInventarioRepository;

    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    public org.springframework.data.domain.Page<Producto> listarTodosPaginado(org.springframework.data.domain.Pageable pageable) {
        return productoRepository.findAll(pageable);
    }

    public List<Producto> listarActivos() {
        return productoRepository.findByActivoTrue();
    }

    public Producto buscarPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
    }

    @Transactional
    public Producto guardar(Producto producto) {
        return productoRepository.save(producto);
    }

    @Transactional
    public void cambiarEstado(Long id) {
        Producto producto = buscarPorId(id);
        producto.setActivo(!producto.isActivo());
        productoRepository.save(producto);
    }

    // Alerta que usarás en el Dashboard (Punto 4 de tus requerimientos)
    public List<Producto> verificarAlertasStock() {
        return productoRepository.findByStockLessThanEqualAndActivoTrue(3);
    }

    @Transactional
    public void registrarCompra(CompraProveedor compra) {
        Producto producto = productoRepository.findById(compra.getProducto().getId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        int stockAIncrementar = 0;
        if ("PAQUETE".equals(compra.getTipoCompra().name())) {
            stockAIncrementar = compra.getCantidadPaquetes() * compra.getUnidadesPorPaquete();
        } else {
            stockAIncrementar = compra.getCantidadPaquetes();
        }

        compra.setTotalUnidades(stockAIncrementar);
        compra.setTotalInvertido(compra.getCantidadPaquetes() * compra.getPrecioCompraPaquete());

        producto.setStock(producto.getStock() + stockAIncrementar);
        producto.setPrecioVenta(compra.getPrecioVentaUnidad());

        productoRepository.save(producto);
        CompraProveedor compraGuardada = compraProveedorRepository.save(compra);

        HistorialInventario movimiento = new HistorialInventario();
        movimiento.setProducto(producto);
        movimiento.setTipoMovimiento("ENTRADA");
        movimiento.setCantidad(stockAIncrementar);
        movimiento.setStockResultante(producto.getStock());
        movimiento.setMotivo("Abastecimiento - Proveedor: " + 
                (compraGuardada.getProveedor() != null ? compraGuardada.getProveedor().getNombre() : "Compra Directa"));
        
        historialInventarioRepository.save(movimiento);
    }
    
}