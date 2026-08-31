package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.model.Categoria;
import com.example.BarberiaLaClasica.model.Promocion;
import com.example.BarberiaLaClasica.repository.CategoriaRepository;
import com.example.BarberiaLaClasica.repository.ProductoRepository;
import com.example.BarberiaLaClasica.repository.ServicioRepository;
import com.example.BarberiaLaClasica.service.ProductoService;
import com.example.BarberiaLaClasica.service.PromocionService;
import com.example.BarberiaLaClasica.service.ServicioService;
import com.example.BarberiaLaClasica.repository.PromocionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/promociones")
public class PromocionController {

    @Autowired
    private PromocionService promocionService;
    @Autowired
    private PromocionRepository promocionRepository;
    @Autowired
    private ServicioRepository servicioRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private ServicioService servicioService;
    @Autowired
    private ProductoService productoService;

    // ── 1. Listar todas las promociones en el Panel ─────────────────────────
    @GetMapping
    public String listarPromociones(Model model) {
        model.addAttribute("promocion", new Promocion());
        model.addAttribute("promociones", promocionService.listarTodas());
        model.addAttribute("servicios", servicioService.listarTodos());
        model.addAttribute("productos", productoService.listarTodos());

        // CORRECCIÓN: Filtramos para obtener las Subcategorías válidas
        List<Categoria> subcategoriasProductos = categoriaRepository.findAll().stream()
                // 1. Asegura que sea una subcategoría (tiene un padre asignado)
                .filter(cat -> cat.getPadre() != null)
                // 2. Opcional: Filtra para asegurarte de que el nombre del papá sea el de
                // Productos
                .filter(cat -> "Productos de Barbería".equalsIgnoreCase(cat.getPadre().getNombre())
                        || "Productos".equalsIgnoreCase(cat.getPadre().getNombre()))
                .collect(Collectors.toList());

        // Enviamos la lista filtrada con el nombre exacto que espera tu archivo HTML
        model.addAttribute("categoriasConSubcategorias", subcategoriasProductos);

        return "promociones/lista"; // O la ruta exacta de tu vista
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("promocion", new Promocion());

        // Filtrado Inteligente basado en tu entidad Autorreferencial
        List<Categoria> categoriasFiltradas = categoriaRepository.findAll().stream()
                // 1. Asegura que sea una Categoría Padre/Principal (no tiene papá)
                .filter(cat -> cat.getPadre() == null)
                // 2. Asegura que tenga subcategorías asignadas (lista no vacía)
                .filter(cat -> cat.getSubcategorias() != null && !cat.getSubcategorias().isEmpty())
                .collect(Collectors.toList());

        model.addAttribute("categoriasConSubcategorias", categoriasFiltradas);
        return "admin/promociones-formulario";
    }

    // ── 2. Guardar o Editar Promoción ───────────────────────────────────────
    @PostMapping("/guardar")
    public String guardarPromocion(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) String tipoPromocion, 
            @RequestParam(required = false) BigDecimal porcentajeDescuento, 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio, 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin, 
            @RequestParam(defaultValue = "0") int minimoVisitasRequeridas,
            @RequestParam(required = false) Long servicioId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long categoriaId,
            RedirectAttributes ra) {

        try {
            // 1. VALIDACIÓN DE CAMPOS VACÍOS
            if (nombre == null || nombre.trim().isEmpty() ||
                    descripcion == null || descripcion.trim().isEmpty() ||
                    tipoPromocion == null || tipoPromocion.trim().isEmpty() ||
                    porcentajeDescuento == null || fechaInicio == null || fechaFin == null) {
                throw new RuntimeException("Todos los campos obligatorios deben ser completados.");
            }

            // 2. VALIDACIÓN DE SELECCIÓN DE OBJETIVO (SERVICIO / PRODUCTO / CATEGORÍA)
            if ("SERVICIO".equalsIgnoreCase(tipoPromocion)) {
                if (servicioId == null) {
                    throw new RuntimeException("Debe seleccionar un servicio válido para aplicar la promoción.");
                }
            } else if ("PRODUCTO".equalsIgnoreCase(tipoPromocion)) {
                if (productoId == null && categoriaId == null) {
                    throw new RuntimeException("Debe seleccionar un producto específico o una categoría para aplicar la promoción.");
                }
            }

            // 3. CANDADO DE PORCENTAJES LÓGICOS
            if (porcentajeDescuento.compareTo(BigDecimal.ONE) < 0 || porcentajeDescuento.compareTo(new BigDecimal("80")) > 0) {
                throw new RuntimeException("El porcentaje de descuento debe estar entre 1% y 80%.");
            }

            // 4. VALIDACIÓN DE FECHAS
            if (!fechaFin.isAfter(fechaInicio)) {
                throw new RuntimeException("La fecha de fin debe ser posterior a la fecha de inicio.");
            }

            if (fechaFin.isBefore(LocalDateTime.now())) {
                throw new RuntimeException("La fecha de fin no puede ser una fecha o tiempo pasado.");
            }

            // ── 🛟 CANDADO ANTIMULTIPLICIDAD: EVITAR SOLAPAMIENTO DE FECHAS ──
            List<Promocion> todas = promocionRepository.findAll();
            for (Promocion p : todas) {
                // Si estamos editando la misma promoción, ignoramos la validación consigo misma
                if (id != null && p.getId().equals(id)) {
                    continue;
                }

                // Solo validamos si la promoción iterada está activa/vigente
                if (p.isActivo() && p.getFechaFin().isAfter(LocalDateTime.now())) {
                    
                    boolean seCruza = (fechaInicio.isBefore(p.getFechaFin()) && fechaFin.isAfter(p.getFechaInicio()));

                    if (seCruza) {
                        if ("SERVICIO".equalsIgnoreCase(tipoPromocion) && p.getServicio() != null && p.getServicio().getId().equals(servicioId)) {
                            throw new RuntimeException("El servicio '" + p.getServicio().getNombre() + "' ya cuenta con una promoción activa en ese rango de fechas.");
                        }
                        if ("PRODUCTO".equalsIgnoreCase(tipoPromocion)) {
                            if (productoId != null && p.getProducto() != null && p.getProducto().getId().equals(productoId)) {
                                throw new RuntimeException("El producto '" + p.getProducto().getNombre() + "' ya tiene una promoción activa en ese rango de fechas.");
                            }
                            if (categoriaId != null && p.getCategoria() != null && p.getCategoria().getId().equals(categoriaId)) {
                                throw new RuntimeException("La categoría seleccionada ya tiene una promoción activa en ese rango de fechas.");
                            }
                        }
                    }
                }
            }

            // Lógica de persistencia existente
            Promocion promo;
            if (id != null) {
                promo = promocionRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Promoción no encontrada"));
            } else {
                promo = new Promocion();
            }

            promo.setNombre(nombre);
            promo.setDescripcion(descripcion);
            promo.setTipoPromocion(tipoPromocion);
            promo.setPorcentajeDescuento(porcentajeDescuento);
            promo.setFechaInicio(fechaInicio);
            promo.setFechaFin(fechaFin);
            promo.setMinimoVisitasRequeridas("PRODUCTO".equalsIgnoreCase(tipoPromocion) ? 0 : minimoVisitasRequeridas);

            if ("SERVICIO".equalsIgnoreCase(tipoPromocion)) {
                promo.setServicio(servicioRepository.findById(servicioId)
                        .orElseThrow(() -> new RuntimeException("Servicio seleccionado no encontrado")));
                promo.setProducto(null);
                promo.setCategoria(null);
            } else { 
                if (productoId != null) {
                    promo.setProducto(productoRepository.findById(productoId)
                            .orElseThrow(() -> new RuntimeException("Producto seleccionado no encontrado")));
                    promo.setCategoria(null);
                } else if (categoriaId != null) {
                    promo.setCategoria(categoriaRepository.findById(categoriaId)
                            .orElseThrow(() -> new RuntimeException("Categoría seleccionada no encontrada")));
                    promo.setProducto(null);
                }
                promo.setServicio(null);
            }

            promocionRepository.save(promo);
            ra.addFlashAttribute("exito", "Promoción guardada correctamente.");

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/promociones";
    }

    // ── 3. Pausar o Activar (Toggle) sin borrar ─────────────────────────────
    @GetMapping("/toggle/{id}")
    public String togglePromocion(@PathVariable Long id, RedirectAttributes ra) {
        try {
            promocionService.toggleActivo(id);
            ra.addFlashAttribute("exito", "Estado de la promoción actualizado.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/promociones";
    }

    // ── 4. Eliminar definitivamente ─────────────────────────────────────────
    @GetMapping("/eliminar/{id}")
    public String eliminarPromocion(@PathVariable Long id, RedirectAttributes ra) {
        try {
            promocionService.eliminar(id);
            ra.addFlashAttribute("exito", "Promoción eliminada con éxito.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/promociones";
    }
}