package com.example.BarberiaLaClasica.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.BarberiaLaClasica.model.Producto;
import com.example.BarberiaLaClasica.repository.ProductoRepository;
import com.example.BarberiaLaClasica.service.PedidoService;
import com.example.BarberiaLaClasica.service.PromocionHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;

@Controller
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private PromocionHelper promocionHelper;

    // ── Página de pago del carrito ────────────────────────
    @GetMapping("/cliente/carrito/pago")
    public String pantallaPago(
            @RequestParam(required = false) String carritoData,
            Model model, Principal principal) {

        if (principal == null)
            return "redirect:/cliente/login";

        // Si no viene carritoData, la página lo carga desde localStorage via JS
        model.addAttribute("items", new ArrayList<>());
        model.addAttribute("total", 0.0);
        return "cliente/carrito-pago";
    }

    @PostMapping("/cliente/carrito/confirmar")
    public String confirmarPedido(
            @RequestParam("comprobante") MultipartFile comprobante,
            @RequestParam("carritoJson") String carritoJson,
            HttpSession session,
            Principal principal,
            Model model,
            RedirectAttributes ra) {

        if (principal == null)
            return "redirect:/cliente/login";

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> itemsRaw = mapper.readValue(carritoJson,
                    new TypeReference<>() {
                    });

            // Validar que el carrito no llegue vacío
            if (itemsRaw == null || itemsRaw.isEmpty())
                throw new RuntimeException("El carrito está vacío");

            // ── ENRIQUECER EN CALIENTE ANTES DE GUARDAR EN BASE DE DATOS ──
            List<Map<String, Object>> itemsConDescuento = new ArrayList<>();
            for (Map<String, Object> itemRaw : itemsRaw) {
                Long id = Long.parseLong(itemRaw.get("id").toString());
                int cantidad = Integer.parseInt(itemRaw.get("cantidad").toString());

                Producto p = productoRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

                // Calculamos el precio real con descuento usando tu helper
                double precioReal = promocionHelper.calcularPrecioProducto(p);

                Map<String, Object> itemProcesado = new HashMap<>();
                itemProcesado.put("id", p.getId());
                itemProcesado.put("cantidad", cantidad);
                itemProcesado.put("precio", precioReal); // ✅ Enviamos el precio de oferta al servicio
                itemProcesado.put("subtotal", precioReal * cantidad);

                itemsConDescuento.add(itemProcesado);
            }

            // Le pasamos la lista ya corregida con los precios de promoción al servicio
            pedidoService.confirmarPedido(principal.getName(), itemsConDescuento, comprobante);
            session.removeAttribute("carrito");

            ra.addFlashAttribute("exito", true);
            return "redirect:/cliente/mis-pedidos?exito=true";

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            // Recargar los ítems usando el método corregido que ya configuramos con
            // PromocionHelper
            List<Map<String, Object>> carrito = obtenerCarrito(session);
            recargarItems(carrito, model);
            return "cliente/carrito-pago";
        }
    }

    @GetMapping("/secretario/pedidos/aceptados")
    public String pedidosAceptados(Model model) {
        model.addAttribute("pedidos",
                pedidoService.listarPorEstado(2)); // estado 2 = aceptados
        return "secretario/pedidos-aceptados";
    }

    @PostMapping("/secretario/pedidos/{id}/entregar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> entregar(@PathVariable Long id) {
        try {
            pedidoService.marcarEntregado(id);
            return ResponseEntity.ok(Map.of("ok", "true"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/carrito/enriquecer")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> enriquecerCarrito(
            @RequestBody List<Map<String, Object>> items) {
        try {
            List<Map<String, Object>> enriquecidos = new ArrayList<>();
            double total = 0;

            for (Map<String, Object> item : items) {
                Long id = Long.parseLong(item.get("id").toString());
                int cantidad = Integer.parseInt(item.get("cantidad").toString());

                Producto p = productoRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

                if (p.getStock() < cantidad)
                    throw new RuntimeException(
                            "Stock insuficiente para: " + p.getNombre() +
                                    ". Solo hay " + p.getStock() + " disponibles.");

                // ── CÁLCULO DE PROMOCIÓN CON EL HELPER ──
                double precioConDescuento = promocionHelper.calcularPrecioProducto(p);
                double subtotalItem = precioConDescuento * cantidad;

                Map<String, Object> e = new HashMap<>();
                e.put("id", p.getId());
                e.put("nombre", p.getNombre());
                e.put("imagen", p.getImagen());
                e.put("precio", precioConDescuento); // ✅ Precio de oferta real
                e.put("precioOriginal", p.getPrecioVenta()); // ✅ Enviamos el original para el tachado en JS
                e.put("cantidad", cantidad);
                e.put("subtotal", subtotalItem);

                enriquecidos.add(e);
                total += subtotalItem;
            }

            return ResponseEntity.ok(Map.of(
                    "items", enriquecidos,
                    "total", total));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private void recargarItems(List<Map<String, Object>> carrito, Model model) {
        List<Map<String, Object>> items = new ArrayList<>();
        double total = 0;
        for (Map<String, Object> item : carrito) {
            Long id = Long.parseLong(item.get("id").toString());
            productoRepository.findById(id).ifPresent(p -> {
                double precioConDescuento = promocionHelper.calcularPrecioProducto(p);
                int cantidad = Integer.parseInt(item.get("cantidad").toString());

                Map<String, Object> e = new HashMap<>(item);
                e.put("nombre", p.getNombre());
                e.put("imagen", p.getImagen());
                e.put("precio", precioConDescuento);
                e.put("precioOriginal", p.getPrecioVenta());
                e.put("subtotal", precioConDescuento * cantidad);
                items.add(e);
            });
            if (!items.isEmpty())
                total += (double) items.get(items.size() - 1).get("subtotal");
        }
        model.addAttribute("items", items);
        model.addAttribute("total", total);
    }

    // ── Historial de pedidos del cliente ──────────────────
    @GetMapping("/cliente/mis-pedidos")
    public String misPedidos(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/cliente/login";
        model.addAttribute("pedidos", pedidoService.historialCliente(principal.getName()));
        return "cliente/mis-pedidos";
    }

    // ── API: agregar al carrito ───────────────────────────
    @PostMapping("/api/carrito/agregar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> agregarAlCarrito(
            @RequestBody Map<String, Object> datos,
            HttpSession session) {

        Long productoId = Long.parseLong(datos.get("productoId").toString());
        int cantidad = Integer.parseInt(datos.get("cantidad").toString());

        Producto producto = productoRepository.findById(productoId)
                .orElse(null);
        if (producto == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Producto no encontrado"));

        List<Map<String, Object>> carrito = obtenerCarrito(session);

        // Buscar si ya existe en carrito
        Map<String, Object> existente = carrito.stream()
                .filter(i -> i.get("id").toString().equals(productoId.toString()))
                .findFirst().orElse(null);

        int cantidadActual = existente != null
                ? Integer.parseInt(existente.get("cantidad").toString())
                : 0;
        int nuevaCantidad = cantidadActual + cantidad;

        // Validar stock
        if (nuevaCantidad > producto.getStock()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Solo hay " + producto.getStock() +
                            " unidades disponibles de " + producto.getNombre()));
        }

        if (existente != null) {
            existente.put("cantidad", nuevaCantidad);
        } else {
            Map<String, Object> nuevoItem = new HashMap<>();
            nuevoItem.put("id", productoId);
            nuevoItem.put("cantidad", cantidad);
            carrito.add(nuevoItem);
        }

        session.setAttribute("carrito", carrito);

        int totalItems = carrito.stream()
                .mapToInt(i -> Integer.parseInt(i.get("cantidad").toString())).sum();

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "totalItems", totalItems,
                "mensaje", "'" + producto.getNombre() + "' agregado al carrito"));
    }

    // ── API: obtener cantidad carrito ─────────────────────
    @GetMapping("/api/carrito/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> contarCarrito(HttpSession session) {
        List<Map<String, Object>> carrito = obtenerCarrito(session);
        int total = carrito.stream()
                .mapToInt(i -> Integer.parseInt(i.get("cantidad").toString())).sum();
        return ResponseEntity.ok(Map.of("totalItems", total));
    }

    // ── Secretario: ver pedidos ───────────────────────────
    @GetMapping("/secretario/pedidos")
    public String pedidosSecretario(Model model) {
        model.addAttribute("pedidos", pedidoService.listarTodos());
        return "secretario/pedidos";
    }

    @PostMapping("/api/carrito/sincronizar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sincronizarCarrito(
            @RequestBody List<Map<String, Object>> items,
            HttpSession session) {
        // Guarda el carrito en sesión para cuando vaya a pagar
        session.setAttribute("carrito", items.stream().map(item -> {
            Map<String, Object> i = new HashMap<>();
            i.put("id", item.get("id"));
            i.put("cantidad", item.get("cantidad"));
            return i;
        }).collect(java.util.stream.Collectors.toList()));

        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/secretario/pedidos/{id}/aceptar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> aceptar(
            @PathVariable Long id) {
        try {
            pedidoService.aceptarPedido(id);
            return ResponseEntity.ok(Map.of("ok", "true"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/secretario/pedidos/{id}/rechazar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> rechazar(
            @PathVariable Long id) {
        try {
            pedidoService.rechazarPedido(id);
            return ResponseEntity.ok(Map.of("ok", "true"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> obtenerCarrito(HttpSession session) {
        Object c = session.getAttribute("carrito");
        if (c instanceof List)
            return (List<Map<String, Object>>) c;
        List<Map<String, Object>> nuevo = new ArrayList<>();
        session.setAttribute("carrito", nuevo);
        return nuevo;
    }

    @GetMapping("/api/auth/check")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkAuth(Principal principal) {
        return ResponseEntity.ok(Map.of("logueado", principal != null));
    }
}
