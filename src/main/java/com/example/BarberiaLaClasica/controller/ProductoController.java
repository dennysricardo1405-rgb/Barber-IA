package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.model.CompraProveedor;
import com.example.BarberiaLaClasica.model.Producto;
import com.example.BarberiaLaClasica.model.Proveedor;
import com.example.BarberiaLaClasica.repository.CategoriaRepository;
import com.example.BarberiaLaClasica.repository.CompraProveedorRepository;
import com.example.BarberiaLaClasica.service.ProductoService;
import com.example.BarberiaLaClasica.service.ProveedorService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin/productos") // <-- Esta es la ruta base de TODO el controlador
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ProveedorService proveedorService;

    @Autowired
    private CompraProveedorRepository compraProveedorRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    // 1. LISTAR PRODUCTOS CON PAGINACIÓN
    @GetMapping("")
    public String listarProductos(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("id").descending());
        org.springframework.data.domain.Page<Producto> productosPage = productoService.listarTodosPaginado(pageable);

        model.addAttribute("productosPage", productosPage);
        model.addAttribute("productos", productosPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productosPage.getTotalPages());
        model.addAttribute("totalItems", productosPage.getTotalElements());
        model.addAttribute("size", size);

        // 1. Enviamos solo las categorías principales (las que no tienen padre)
        model.addAttribute("categoriasPadre", categoriaRepository.findByPadreIsNullAndActivoTrue());

        // 2. Enviamos todas las subcategorías activas del sistema
        model.addAttribute("subcategorias", categoriaRepository.findByPadreIsNotNullAndActivoTrue());

        model.addAttribute("nuevoProducto", new Producto());
        return "productos/lista";
    }

    // 2. GUARDAR / EDITAR PRODUCTO (Mapea a: /admin/productos/guardar)
    @PostMapping("/guardar")
    public String guardarProducto(@ModelAttribute("nuevoProducto") Producto producto,
            @RequestParam("imagenFile") MultipartFile file) {
        try {
            if (!file.isEmpty()) {
                String rutaBase = System.getProperty("user.dir") + "/uploads/productos";
                Path directorioPath = Paths.get(rutaBase);

                if (!Files.exists(directorioPath)) {
                    Files.createDirectories(directorioPath);
                }

                String nombreUnicoArchivo = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                Path rutaAbsoluta = directorioPath.resolve(nombreUnicoArchivo);

                Files.copy(file.getInputStream(), rutaAbsoluta);

                producto.setImagen("/uploads/productos/" + nombreUnicoArchivo);

            } else if (producto.getId() != null) {
                Producto productoExistente = productoService.buscarPorId(producto.getId());
                if (productoExistente != null) {
                    producto.setImagen(productoExistente.getImagen());
                }
            }

            if (producto.getPrecioVenta() > 999.99) {
                producto.setPrecioVenta(999.99);
            }

            productoService.guardar(producto);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return "redirect:/admin/productos?exito";
    }

    // 3. SWITCH DE ESTADO (Mapea a: /admin/productos/estado/{id})
    @GetMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable("id") Long id) {
        productoService.cambiarEstado(id);
        return "redirect:/admin/productos?estadoCambiado";
    }

    // VISTAS PARA EL FORMULARIO DE COMPRAS A PROVEEDOR
    @GetMapping("/compras")
    public String vistaCompras(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
            
        // Creamos la paginación de 10 en 10 registros
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        
        // Traemos la página de compras desde el repositorio
        org.springframework.data.domain.Page<CompraProveedor> comprasPage = compraProveedorRepository.findAllByOrderByIdDesc(pageable);

        model.addAttribute("productos", productoService.listarActivos());
        model.addAttribute("proveedores", proveedorService.listarTodos());
        
        // Pasamos el contenido pálido de la página actual a la tabla
        model.addAttribute("historialCompras", comprasPage.getContent());

        // Atributos obligatorios para armar el paginador en el HTML
        model.addAttribute("comprasPage", comprasPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", comprasPage.getTotalPages());
        model.addAttribute("totalItems", comprasPage.getTotalElements());
        model.addAttribute("size", size);

        model.addAttribute("compra", new CompraProveedor());
        return "productos/compras"; 
    }

    // 2. Procesa el formulario. URL de acción: /admin/productos/compras/guardar
    @PostMapping("/compras/guardar")
    public String guardarCompra(
            @ModelAttribute("compra") CompraProveedor compra,
            @RequestParam(value = "esCompraDirecta", defaultValue = "false") boolean esCompraDirecta) {

        compra.setEsCompraDirecta(esCompraDirecta);

        if (esCompraDirecta) {
            compra.setProveedor(null);
        } else {
            if (compra.getProveedor() != null && compra.getProveedor().getId() == null) {
                compra.setProveedor(null);
            }
        }

        productoService.registrarCompra(compra);

        // Redirecciona a la URL del GetMapping para refrescar la pantalla y mostrar la
        // tabla limpia
        return "redirect:/admin/productos/compras?compraExitosa";
    }
}