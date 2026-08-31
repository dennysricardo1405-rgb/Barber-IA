package com.example.BarberiaLaClasica.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

import org.springframework.transaction.annotation.Transactional;

import com.example.BarberiaLaClasica.service.BarberoService;
import com.example.BarberiaLaClasica.service.ProductoService;
import com.example.BarberiaLaClasica.service.PromocionHelper;
import com.example.BarberiaLaClasica.service.RecepcionService;
import com.example.BarberiaLaClasica.service.ClienteService;
import com.example.BarberiaLaClasica.service.CategoriaService;
import com.example.BarberiaLaClasica.repository.ServicioRepository;
import com.example.BarberiaLaClasica.repository.DetalleNotaVentaRepository;
import com.example.BarberiaLaClasica.repository.NotaVentaRepository;
import com.example.BarberiaLaClasica.model.Barbero;
import com.example.BarberiaLaClasica.model.Categoria;
import com.example.BarberiaLaClasica.model.Cita;
import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.model.ConsumoSilla;
import com.example.BarberiaLaClasica.model.DetalleNotaVenta;
import com.example.BarberiaLaClasica.model.NotaVenta;
import com.example.BarberiaLaClasica.model.Producto;

@Controller
@RequestMapping("/secretario")
public class RecepcionController {

    @Autowired
    private BarberoService barberoService;
    @Autowired
    private ServicioRepository servicioRepository;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private ClienteService clienteService;
    @Autowired
    private RecepcionService recepcionService;
    @Autowired
    private PromocionHelper promocionHelper;
    @Autowired
    private CategoriaService categoriaService;
    @Autowired
    private DetalleNotaVentaRepository detalleNotaVentaRepository;
    @Autowired
    private NotaVentaRepository notaVentaRepository;

    // ── Panel principal con PAGINACIÓN ───────────────────────────────────────
    @GetMapping("/recepcion")
    public String verPanelRecepcion(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<Barbero> barberos = barberoService.listarTodos();

        Map<Long, Cita> reservasHoy = new HashMap<>();
        Map<Long, Boolean> enSesion = new HashMap<>();

        for (Barbero b : barberos) {
            recepcionService.getCitaReservaHoy(b.getId())
                    .ifPresent(c -> reservasHoy.put(b.getId(), c));
            recepcionService.getSessionActiva(b.getId())
                    .ifPresent(s -> enSesion.put(b.getId(), true));
        }

        // Paginación para Reservas Web de Hoy
        Pageable pageable = PageRequest.of(page, size, Sort.by("horaInicio").ascending());
        Page<Cita> reservasPage = recepcionService.listarReservasHoyPaginadas(pageable);

        model.addAttribute("barberos", barberos);
        model.addAttribute("reservasHoy", reservasHoy);
        model.addAttribute("enSesion", enSesion);
        model.addAttribute("servicios", servicioRepository.findByEstado(1));
        model.addAttribute("productos", productoService.listarTodos());
        model.addAttribute("clientesExistentes", clienteService.listarTodos());

        // Atributos de paginación
        model.addAttribute("reservasPage", reservasPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reservasPage.getTotalPages());
        model.addAttribute("totalItems", reservasPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("promoHelper", promocionHelper);
        return "secretario/recepcion";
    }

    // ── Atender reserva existente ─────────────────────────────────────────────
    @PostMapping("/recepcion/atender-reserva/{barberoId}")
    public String atenderReserva(@PathVariable Long barberoId, RedirectAttributes ra) {
        try {
            recepcionService.atenderReserva(barberoId);
            ra.addFlashAttribute("exito", "Sesión iniciada desde reserva.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/secretario/recepcion";
    }

    // ── Walk-in: ocupar silla manualmente ────────────────────────────────────
    @PostMapping("/recepcion/ocupar-silla")
    public String ocuparSilla(
            @RequestParam Long barberoId,
            @RequestParam Long servicioId,
            @RequestParam(required = false) Long clienteId,
            RedirectAttributes ra) {
        try {
            recepcionService.ocuparSillaWalkin(barberoId, clienteId, servicioId);
            ra.addFlashAttribute("exito", "¡Estación abierta!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/secretario/recepcion";
    }

    // ── API consumos ──────────────────────────────────────────────────────────
    @GetMapping("/recepcion/api-consumos/{barberoId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> consumos(@PathVariable Long barberoId) {
        List<ConsumoSilla> consumos = recepcionService.obtenerConsumos(barberoId);

        // 1. Mapeamos los productos consumidos (aquí se mantiene el precio normal de
        // venta)
        List<Map<String, Object>> items = consumos.stream().map(c -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            if (c.getProducto() != null) {
                double precioOriginal = c.getProducto().getPrecioVenta();
                double subtotalCalculado = precioOriginal * c.getCantidad();
                item.put("tipo", "PRODUCTO");
                item.put("descripcion", c.getProducto().getNombre());
                item.put("cantidad", c.getCantidad());
                item.put("precioUnit", precioOriginal);
                item.put("precioOriginal", precioOriginal);
                item.put("subtotal", subtotalCalculado);
            } else if (c.getServicio() != null) {
                double precioOriginal = c.getServicio().getPrecio().doubleValue();
                double precioFinal = c.getSubtotal();
                item.put("tipo", "SERVICIO");
                item.put("servicioId", c.getServicio().getId());
                item.put("descripcion", c.getServicio().getNombre());
                item.put("cantidad", 1);
                item.put("precioUnit", precioFinal);
                item.put("precioOriginal", precioOriginal);
                item.put("subtotal", precioFinal);
            }
            return item;
        }).toList();

        double totalProductos = items.stream().mapToDouble(i -> (double) i.get("subtotal")).sum();

        Map<String, Object> response = new HashMap<>();
        response.put("consumos", items);
        response.put("anticipoYape", 0.0);
        response.put("codigoYape", "");

        final double[] totalFinal = { totalProductos };

        // 2. Lógica para el Servicio de la Silla (Promociones solo aplican a Reservas Web)
        recepcionService.getSessionActiva(barberoId).ifPresent(s -> {
            double precioServicioOriginal = s.getServicio().getPrecio().doubleValue();
            
            double precioServicioFinal = precioServicioOriginal;
            if (s.getCita() != null && s.getCita().getTotalPrecio() != null) {
                precioServicioFinal = s.getCita().getTotalPrecio().doubleValue();
            }

            if (s.getCita() != null && s.getCita().getMontoYape() != null
                    && s.getCita().getMontoYape().compareTo(java.math.BigDecimal.ZERO) > 0) {
                response.put("anticipoYape", s.getCita().getMontoYape().doubleValue());
                response.put("codigoYape", s.getCita().getCodigoYape() != null ? s.getCita().getCodigoYape() : "");
            }

            totalFinal[0] += precioServicioFinal;

            Map<String, Object> servicioMap = new HashMap<>();
            servicioMap.put("id", s.getServicio().getId());
            servicioMap.put("nombre", s.getServicio().getNombre());
            servicioMap.put("precio", precioServicioFinal);
            servicioMap.put("precioOriginal", precioServicioOriginal);
            response.put("servicio", servicioMap);

            if (s.getCliente() != null) {
                Map<String, Object> clienteMap = new HashMap<>();
                clienteMap.put("nombres", s.getCliente().getNombres());
                clienteMap.put("apellidos", s.getCliente().getApellidos());
                clienteMap.put("dni", s.getCliente().getDni());
                response.put("cliente", clienteMap);
            }
        });

        response.put("total", totalFinal[0]);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/recepcion/api-consumos/agregar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> agregar(
            @RequestParam Long barberoId,
            @RequestParam Long productoId,
            @RequestParam int cantidad) {
        try {
            recepcionService.agregarProducto(barberoId, productoId, cantidad);
            return ResponseEntity.ok(Map.of("mensaje", "Producto agregado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/recepcion/api-consumos/agregar-servicio")
    @ResponseBody
    public ResponseEntity<Map<String, String>> agregarServicio(
            @RequestParam Long barberoId,
            @RequestParam Long servicioId) {
        try {
            recepcionService.agregarServicio(barberoId, servicioId);
            return ResponseEntity.ok(Map.of("mensaje", "Servicio agregado a la silla"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/recepcion/api-consumos/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable Long id) {
        recepcionService.quitarConsumo(id);
        return ResponseEntity.ok(Map.of("mensaje", "Eliminado"));
    }

    // ── Finalizar ─────────────────────────────────────────────────────────────
    @GetMapping("/recepcion/finalizar-pago/{barberoId}")
    public String finalizar(
            @PathVariable Long barberoId,
            @RequestParam(required = false, defaultValue = "EFECTIVO") String metodoPago,
            @RequestParam(required = false, defaultValue = "0") double montoYape,
            @RequestParam(required = false) String codigoYape,
            RedirectAttributes ra) {
        try {
            // 4. Tu flujo original procesará el cobro con el valor promocional sin alterar la BD
            NotaVenta nota = recepcionService.finalizarAtencion(
                    barberoId, metodoPago, montoYape, codigoYape);

            ra.addFlashAttribute("exito", "Nota de venta #" + nota.getId() + " generada.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/secretario/recepcion";
    }

    // ── Notas de venta ────────────────────────────────────────────────────────
    @GetMapping("/recepcion/notas-venta/{id}/detalle")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> detalleNota(@PathVariable Long id) {
        NotaVenta nota = recepcionService.obtenerNota(id);

        List<Map<String, Object>> detalles = nota.getDetalles().stream().map(d -> {
            Map<String, Object> item = new HashMap<>();
            item.put("descripcion", d.getDescripcion() != null ? d.getDescripcion() : "");
            item.put("cantidad", d.getCantidad());
            item.put("precioUnitario", d.getPrecioUnitario());
            item.put("subtotal", d.getSubtotal());
            item.put("tipo", d.getTipo() != null ? d.getTipo() : "PRODUCTO");
            return item;
        }).toList();

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", nota.getId());
        resp.put("fecha", nota.getFecha() != null
                ? nota.getFecha().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "");
        resp.put("cliente", nota.getCliente() != null
                ? nota.getCliente().getNombres() + " " + nota.getCliente().getApellidos()
                : null);
        resp.put("barbero", nota.getBarbero() != null ? nota.getBarbero().getNombre() : null);
        resp.put("total", nota.getTotal());
        resp.put("detalles", detalles);

        // ── 💵 CAMPOS AUDITORÍA FINANCIERA ──
        resp.put("metodoPago", nota.getMetodoPago());
        resp.put("montoEfectivo", nota.getMontoEfectivo());
        resp.put("montoYape", nota.getMontoYape());
        resp.put("codigoYape", nota.getCodigoYape());

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/recepcion/asociar-cliente")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, String>> asociarCliente(
            @RequestParam Long barberoId,
            @RequestParam Long clienteId) {
        try {
            recepcionService.asociarCliente(barberoId, clienteId);
            return ResponseEntity.ok(Map.of("mensaje", "ok"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── NOTAS DE VENTA CON PAGINACIÓN Y FILTRO POR PERÍODO ─────────────────────
    @GetMapping("/recepcion/notas-venta")
    @Transactional
    public String notas(Model model,
            @RequestParam(defaultValue = "MES") String periodo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
        java.time.LocalDateTime inicio = java.time.LocalDateTime.of(ahora.getYear(), ahora.getMonth(), 1, 0, 0, 0);
        java.time.LocalDateTime fin = ahora.withHour(23).withMinute(59).withSecond(59);

        if ("HOY".equalsIgnoreCase(periodo)) {
            inicio = ahora.withHour(0).withMinute(0).withSecond(0);
        } else if ("SEMANA".equalsIgnoreCase(periodo)) {
            inicio = ahora.minusDays(7).withHour(0).withMinute(0).withSecond(0);
        } else if ("TODOS".equalsIgnoreCase(periodo)) {
            inicio = java.time.LocalDateTime.of(2000, 1, 1, 0, 0);
        }

        List<NotaVenta> todasNotasPeriodo = recepcionService.listarNotasPorRango(inicio, fin);

        double totalRecaudado = todasNotasPeriodo.stream().mapToDouble(NotaVenta::getTotal).sum();
        double totalYape = todasNotasPeriodo.stream().mapToDouble(NotaVenta::getMontoYape).sum();
        double totalEfectivo = todasNotasPeriodo.stream().mapToDouble(NotaVenta::getMontoEfectivo).sum();
        int cantidadNotas = todasNotasPeriodo.size();

        Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
        Page<NotaVenta> notasPage = recepcionService.listarNotasPorRangoPaginadas(inicio, fin, pageable);

        model.addAttribute("notasPage", notasPage);
        model.addAttribute("notas", notasPage.getContent());
        model.addAttribute("periodo", periodo);
        model.addAttribute("totalRecaudado", totalRecaudado);
        model.addAttribute("totalYape", totalYape);
        model.addAttribute("totalEfectivo", totalEfectivo);
        model.addAttribute("cantidadNotas", cantidadNotas);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notasPage.getTotalPages());
        model.addAttribute("totalItems", notasPage.getTotalElements());
        model.addAttribute("size", size);

        return "secretario/notas-venta";
    }

    @GetMapping("/recepcion/ultima-nota")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> ultimaNota(@RequestParam Long barberoId) {
        NotaVenta nota = recepcionService.listarNotas().stream()
                .filter(n -> n.getBarbero() != null &&
                        n.getBarbero().getId().equals(barberoId))
                .findFirst()
                .orElse(null);

        if (nota == null)
            return ResponseEntity.notFound().build();

        List<Map<String, Object>> detalles = nota.getDetalles().stream().map(d -> {
            Map<String, Object> item = new HashMap<>();
            item.put("descripcion", d.getDescripcion() != null ? d.getDescripcion() : "");
            item.put("cantidad", d.getCantidad());
            item.put("precioUnitario", d.getPrecioUnitario());
            item.put("subtotal", d.getSubtotal());
            item.put("tipo", d.getTipo() != null ? d.getTipo() : "SERVICIO");
            return item;
        }).toList();

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", nota.getId());
        resp.put("fecha", nota.getFecha() != null
                ? nota.getFecha().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "");
        resp.put("cliente", nota.getCliente() != null
                ? nota.getCliente().getNombres() + " " + nota.getCliente().getApellidos()
                : null);
        resp.put("barbero", nota.getBarbero() != null ? nota.getBarbero().getNombre() : null);
        resp.put("total", nota.getTotal());
        resp.put("metodoPago", nota.getMetodoPago());
        resp.put("montoYape", nota.getMontoYape());
        resp.put("montoEfectivo", nota.getMontoEfectivo());
        resp.put("codigoYape", nota.getCodigoYape());
        resp.put("detalles", detalles);

        return ResponseEntity.ok(resp);
    }

    // ── VENTA DIRECTA DE PRODUCTOS (MÓDULO DE RECEPCIÓN / CAJA) ───────────────
    @GetMapping("/ventas-productos")
    @Transactional
    public String moduloVentasProductos(Model model,
            @RequestParam(defaultValue = "MES") String periodo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<Categoria> categoriasPrincipales = categoriaService.listarPrincipalesActivas();
        List<Categoria> todasSubcategorias = categoriaService.listarTodasActivas().stream()
                .filter(c -> c.getPadre() != null)
                .toList();
        List<Producto> productosActivos = productoService.listarActivos();
        List<Cliente> clientes = clienteService.listarTodos();

        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
        java.time.LocalDateTime inicio = java.time.LocalDateTime.of(ahora.getYear(), ahora.getMonth(), 1, 0, 0, 0);
        java.time.LocalDateTime fin = ahora.withHour(23).withMinute(59).withSecond(59);

        if ("HOY".equalsIgnoreCase(periodo)) {
            inicio = ahora.withHour(0).withMinute(0).withSecond(0);
        } else if ("SEMANA".equalsIgnoreCase(periodo)) {
            inicio = ahora.minusDays(7).withHour(0).withMinute(0).withSecond(0);
        } else if ("TODOS".equalsIgnoreCase(periodo)) {
            inicio = java.time.LocalDateTime.of(2000, 1, 1, 0, 0);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
        Page<NotaVenta> notasPage = recepcionService.listarNotasPorRangoPaginadas(inicio, fin, pageable);

        // Filtrar notas que tengan detalles tipo PRODUCTO o ventas directas
        List<NotaVenta> ventasProductosHistory = notasPage.getContent().stream()
                .filter(n -> n.getDetalles() != null && n.getDetalles().stream().anyMatch(d -> "PRODUCTO".equalsIgnoreCase(d.getTipo())))
                .toList();

        model.addAttribute("categoriasPrincipales", categoriasPrincipales);
        model.addAttribute("subcategorias", todasSubcategorias);
        model.addAttribute("productos", productosActivos);
        model.addAttribute("clientes", clientes);
        model.addAttribute("ventasHistory", ventasProductosHistory);
        model.addAttribute("notasPage", notasPage);
        model.addAttribute("periodo", periodo);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notasPage.getTotalPages());
        model.addAttribute("size", size);

        return "secretario/ventas-productos";
    }

    @GetMapping("/api/ventas-productos/subcategorias/{padreId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerSubcategorias(@PathVariable Long padreId) {
        List<Categoria> subs = categoriaService.listarSubcategoriasPorPadre(padreId);
        List<Map<String, Object>> result = subs.stream().map(c -> Map.<String, Object>of(
                "id", c.getId(),
                "nombre", c.getNombre())).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/ventas-productos/procesar")
    @ResponseBody
    public ResponseEntity<?> procesarVentaDirecta(@RequestBody Map<String, Object> payload) {
        try {
            Long clienteId = payload.get("clienteId") != null && !payload.get("clienteId").toString().isEmpty()
                    ? Long.valueOf(payload.get("clienteId").toString())
                    : null;

            String metodoPago = payload.get("metodoPago") != null ? payload.get("metodoPago").toString() : "EFECTIVO";
            double montoEfectivo = payload.get("montoEfectivo") != null ? Double.parseDouble(payload.get("montoEfectivo").toString()) : 0.0;
            double montoYape = payload.get("montoYape") != null ? Double.parseDouble(payload.get("montoYape").toString()) : 0.0;
            String codigoYape = payload.get("codigoYape") != null ? payload.get("codigoYape").toString() : "";

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
            if (items == null || items.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Debe agregar al menos un producto al carrito."));
            }

            NotaVenta notaGuardada = recepcionService.procesarVentaDirectaProductos(
                    clienteId, items, metodoPago, montoEfectivo, montoYape, codigoYape);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "notaId", notaGuardada.getId(),
                    "total", notaGuardada.getTotal(),
                    "mensaje", "Venta procesada con éxito. Stock actualizado y registrado en Kardex."));
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errResp = new HashMap<>();
            errResp.put("error", (e.getMessage() != null && !e.getMessage().isEmpty()) ? e.getMessage() : e.toString());
            return ResponseEntity.badRequest().body(errResp);
        }
    }

    // ── MÓDULO INDEPENDIENTE: HISTORIAL DE VENTAS DE PRODUCTOS Y SILLAS (PAGINADO Y FILTRADO) ────────
    @GetMapping("/ventas-productos/historial")
    @Transactional
    public String historialVentasProductos(Model model,
            @RequestParam(defaultValue = "MES") String periodo,
            @RequestParam(defaultValue = "") String origen,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
        java.time.LocalDateTime inicio = java.time.LocalDateTime.of(ahora.getYear(), ahora.getMonth(), 1, 0, 0, 0);
        java.time.LocalDateTime fin = ahora.withHour(23).withMinute(59).withSecond(59);

        if ("HOY".equalsIgnoreCase(periodo)) {
            inicio = ahora.withHour(0).withMinute(0).withSecond(0);
        } else if ("SEMANA".equalsIgnoreCase(periodo)) {
            inicio = ahora.minusDays(7).withHour(0).withMinute(0).withSecond(0);
        } else if ("TODOS".equalsIgnoreCase(periodo)) {
            inicio = java.time.LocalDateTime.of(2000, 1, 1, 0, 0);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
        Page<NotaVenta> notasPage = notaVentaRepository.buscarNotasVentaFiltradas(
                inicio, fin, origen.trim(), search.trim(), pageable);

        // Totales del período
        List<NotaVenta> todasNotasPeriodo = notaVentaRepository.findByFechaBetween(inicio, fin);
        double totalRecaudado = todasNotasPeriodo.stream().mapToDouble(NotaVenta::getTotal).sum();
        long totalVentasSilla = todasNotasPeriodo.stream().filter(n -> n.getBarbero() != null).count();
        long totalVentasCaja  = todasNotasPeriodo.stream().filter(n -> n.getBarbero() == null).count();

        model.addAttribute("notasPage", notasPage);
        model.addAttribute("ventasNotas", notasPage.getContent());
        model.addAttribute("periodo", periodo);
        model.addAttribute("origen", origen);
        model.addAttribute("search", search);
        model.addAttribute("totalRecaudado", totalRecaudado);
        model.addAttribute("totalVentasSilla", totalVentasSilla);
        model.addAttribute("totalVentasCaja", totalVentasCaja);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notasPage.getTotalPages());
        model.addAttribute("totalItems", notasPage.getTotalElements());
        model.addAttribute("size", size);

        return "secretario/ventas-productos-historial";
    }
}