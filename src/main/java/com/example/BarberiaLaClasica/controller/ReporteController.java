package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.model.*;
import com.example.BarberiaLaClasica.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;

@Controller
@RequestMapping("/admin/reportes")
public class ReporteController {

        @Autowired
        private GastoLocalRepository gastoLocalRepository;
        @Autowired
        private CompraProveedorRepository compraProveedorRepository;
        @Autowired
        private CitaRepository citaRepository;
        @Autowired
        private PedidoOnlineRepository pedidoOnlineRepository;
        @Autowired
        private NotaVentaRepository notaVentaRepository;
        @Autowired
        private BarberoRepository barberoRepository;
        @Autowired
        private PagoBarberoRepository pbarberoRepository;
        @Autowired
        private DetalleNotaVentaRepository detalleNotaVentaRepository;

        @GetMapping
        public String verBalanceGeneral(
                        @RequestParam(name = "tipoOperacion", required = false) String tipoOperacion,
                        @RequestParam(name = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                        @RequestParam(name = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
                        @RequestParam(name = "origen", required = false, defaultValue = "TODOS") String origen,
                        @RequestParam(name = "metodoPago", required = false, defaultValue = "TODOS") String metodoPago,
                        Model model) {

                // Inicializamos acumuladores de KPIs en cero (Incluidos los nuevos de
                // Ingresos/Egresos)
                double totalEfectivo = 0.0;
                double totalYape = 0.0;
                double totalMixto = 0.0;
                double totalIngresos = 0.0;
                double totalEgresos = 0.0;
                double totalNeto = 0.0;
                double totalFiltrado = 0.0;

                // Mapas para unificar Citas y Notas de Venta
                List<Map<String, Object>> citasFiltradas = new ArrayList<>();
                List<Map<String, Object>> productosFiltrados = new ArrayList<>();

                // Ajustamos fechas por defecto por si el usuario limpia los calendarios
                LocalDate inicio = (fechaInicio != null) ? fechaInicio : LocalDate.now().withDayOfMonth(1);
                LocalDate fin = (fechaFin != null) ? fechaFin : LocalDate.now();

                // SÓLO PROCESAMOS SI EL USUARIO ELIGIÓ UN TIPO DE OPERACIÓN
                if (tipoOperacion != null && !tipoOperacion.isEmpty()) {

                        if ("SERVICIOS".equals(tipoOperacion)) {

                                List<NotaVenta> notasVenta = notaVentaRepository.findAll();

                                for (NotaVenta nv : notasVenta) {
                                        LocalDate fechaNV = nv.getFecha() != null ? nv.getFecha().toLocalDate() : null;
                                        if (fechaNV == null || fechaNV.isBefore(inicio) || fechaNV.isAfter(fin)) {
                                                continue;
                                        }

                                        // Origen real: si la SillaSession está vinculada a una Cita
                                        boolean vieneDeReserva = nv.getSession() != null
                                                        && nv.getSession().getCita() != null;
                                        String origenRegistro = vieneDeReserva ? "RESERVA_WEB" : "NOTA_VENTA";

                                        double efec = nv.getMontoEfectivo();
                                        double yp = nv.getMontoYape();
                                        String mPago = nv.getMetodoPago() != null ? nv.getMetodoPago() : "EFECTIVO";

                                        boolean pasaCanal = "TODOS".equals(origen)
                                                        || ("WEB".equals(origen) && vieneDeReserva)
                                                        || ("PRESENCIAL".equals(origen) && !vieneDeReserva);

                                        boolean pasaPago = false;
                                        if ("TODOS".equals(metodoPago))
                                                pasaPago = true;
                                        else if ("EFECTIVO".equals(metodoPago) && "EFECTIVO".equals(mPago))
                                                pasaPago = true;
                                        else if ("YAPE".equals(metodoPago) && "YAPE".equals(mPago))
                                                pasaPago = true;
                                        else if ("MIXTO".equals(metodoPago) && "MIXTO".equals(mPago))
                                                pasaPago = true;

                                        if (pasaCanal && pasaPago) {
                                                Map<String, Object> map = new HashMap<>();
                                                map.put("origen", origenRegistro);
                                                map.put("id", nv.getId());
                                                map.put("cliente",
                                                                nv.getCliente() != null ? nv.getCliente().getNombres()
                                                                                : "Cliente sin registro");
                                                map.put("barbero", nv.getBarbero() != null ? nv.getBarbero().getNombre()
                                                                : "No asignado");
                                                map.put("fecha", fechaNV.toString());
                                                map.put("montoEfectivo", efec);
                                                map.put("montoYape", yp);
                                                map.put("totalPrecio", nv.getTotal());

                                                citasFiltradas.add(map);

                                                totalEfectivo += efec;
                                                totalYape += yp;
                                                totalFiltrado += nv.getTotal();
                                        }
                                }

                        } else if ("PRODUCTOS".equals(tipoOperacion)) {
                                // Nota: Asumí que el bloque original manejaba "PRODUCTOS", se ajusta la
                                // condición lógica limpia.
                                if ("TODOS".equals(origen) || "PRESENCIAL".equals(origen)) {

                                        LocalDateTime inicioDT = inicio.atStartOfDay();
                                        LocalDateTime finDT = fin.atTime(23, 59, 59);

                                        List<DetalleNotaVenta> detallesProducto = detalleNotaVentaRepository
                                                        .findByTipoAndFechaBetween("PRODUCTO", inicioDT, finDT);

                                        for (DetalleNotaVenta detalle : detallesProducto) {
                                                NotaVenta nota = detalle.getNotaVenta();
                                                double subtotalItem = detalle.getSubtotal();
                                                String mPago = nota.getMetodoPago() != null ? nota.getMetodoPago()
                                                                : "EFECTIVO";

                                                boolean pasaFiltroPago = false;
                                                if ("TODOS".equals(metodoPago))
                                                        pasaFiltroPago = true;
                                                else if ("EFECTIVO".equals(metodoPago) && "EFECTIVO".equals(mPago))
                                                        pasaFiltroPago = true;
                                                else if ("YAPE".equals(metodoPago) && "YAPE".equals(mPago))
                                                        pasaFiltroPago = true;
                                                else if ("MIXTO".equals(metodoPago) && "MIXTO".equals(mPago))
                                                        pasaFiltroPago = true;

                                                if (pasaFiltroPago) {
                                                        Map<String, Object> map = new HashMap<>();
                                                        map.put("canal", "PRESENCIAL");
                                                        map.put("cliente",
                                                                        nota.getCliente() != null
                                                                                        ? nota.getCliente().getNombres()
                                                                                        : "Cliente Mostrador (Físico)");
                                                        map.put("detalle", detalle.getDescripcion() + " (x"
                                                                        + detalle.getCantidad() + ")");
                                                        map.put("formaPago", mPago);
                                                        map.put("monto", subtotalItem);

                                                        String fechaFormateada = nota.getFecha() != null
                                                                        ? nota.getFecha().format(
                                                                                        java.time.format.DateTimeFormatter
                                                                                                        .ofPattern("dd/MM/yyyy HH:mm"))
                                                                        : "---";
                                                        map.put("fecha", fechaFormateada);

                                                        productosFiltrados.add(map);

                                                        if ("EFECTIVO".equals(mPago)) {
                                                                totalEfectivo += subtotalItem;
                                                        } else if ("YAPE".equals(mPago)) {
                                                                totalYape += subtotalItem;
                                                        } else if ("MIXTO".equals(mPago)) {
                                                                totalMixto += subtotalItem;
                                                        }

                                                        totalFiltrado += subtotalItem;
                                                }
                                        }
                                }

                                // ── B. CANAL WEB: Pedidos Online de tu Ecommerce ──
                                if ("TODOS".equals(origen) || "WEB".equals(origen)) {
                                        List<PedidoOnline> pedidosWeb = pedidoOnlineRepository.findAll();
                                        for (PedidoOnline pOnline : pedidosWeb) {
                                                if (pOnline.getEstado() == 2 || pOnline.getEstado() == 3) {
                                                        double totalPedido = pOnline.getTotal();

                                                        if ("TODOS".equals(metodoPago) || "YAPE".equals(metodoPago)) {
                                                                Map<String, Object> map = new HashMap<>();
                                                                map.put("canal", "WEB");

                                                                String nombreClienteWeb = (pOnline.getCliente() != null)
                                                                                ? pOnline.getCliente().getNombres()
                                                                                : "Usuario Web";
                                                                map.put("cliente", nombreClienteWeb);
                                                                map.put("detalle",
                                                                                "Pedido Ecommerce #" + pOnline.getId());
                                                                map.put("formaPago", "Yape / Transferencia");
                                                                map.put("monto", totalPedido);
                                                                map.put("efectivo", 0.0);
                                                                map.put("yape", totalPedido);

                                                                String fechaWebFormateada = "---";
                                                                if (pOnline.getFechaPedido() != null) {
                                                                        fechaWebFormateada = pOnline.getFechaPedido()
                                                                                        .format(java.time.format.DateTimeFormatter
                                                                                                        .ofPattern("dd/MM/yyyy HH:mm"));
                                                                }
                                                                map.put("fecha", fechaWebFormateada);

                                                                productosFiltrados.add(map);

                                                                totalYape += totalPedido;
                                                                totalFiltrado += totalPedido;
                                                        }
                                                }
                                        }
                                }

                        } else if ("TODOS".equals(tipoOperacion)) {
                                LocalDateTime inicioDT = inicio.atStartOfDay();
                                LocalDateTime finDT = fin.atTime(23, 59, 59);
                                List<Map<String, Object>> movimientos = new ArrayList<>();

                                // ── INGRESOS: Servicios y Productos vendidos presencialmente ──
                                List<DetalleNotaVenta> todosDetalles = detalleNotaVentaRepository
                                                .findByFechaBetween(inicioDT, finDT);
                                for (DetalleNotaVenta detalle : todosDetalles) {
                                        NotaVenta nota = detalle.getNotaVenta();
                                        double subtotalItem = detalle.getSubtotal();

                                        Map<String, Object> mov = new HashMap<>();
                                        mov.put("fechaOrden", nota.getFecha());
                                        mov.put("fecha", nota.getFecha() != null
                                                        ? nota.getFecha()
                                                                        .format(java.time.format.DateTimeFormatter
                                                                                        .ofPattern("dd/MM/yyyy HH:mm"))
                                                        : "---");
                                        mov.put("tipoMovimiento", "INGRESO");
                                        mov.put("categoria",
                                                        "SERVICIO".equals(detalle.getTipo()) ? "Servicio (Presencial)"
                                                                        : "Producto (Presencial)");
                                        mov.put("descripcion",
                                                        detalle.getDescripcion() + " (x" + detalle.getCantidad() + ")");
                                        mov.put("monto", subtotalItem);
                                        movimientos.add(mov);

                                        totalIngresos += subtotalItem;
                                }

                                // ── INGRESOS: Pedidos Ecommerce Web ──
                                List<PedidoOnline> pedidosWeb = pedidoOnlineRepository.findAll();
                                for (PedidoOnline pOnline : pedidosWeb) {
                                        if (pOnline.getEstado() == 2 || pOnline.getEstado() == 3) {
                                                LocalDate fechaPedido = pOnline.getFechaPedido() != null
                                                                ? pOnline.getFechaPedido().toLocalDate()
                                                                : null;
                                                if (fechaPedido == null || fechaPedido.isBefore(inicio)
                                                                || fechaPedido.isAfter(fin)) {
                                                        continue;
                                                }
                                                double totalPedido = pOnline.getTotal();

                                                Map<String, Object> mov = new HashMap<>();
                                                mov.put("fechaOrden", pOnline.getFechaPedido());
                                                mov.put("fecha", pOnline.getFechaPedido()
                                                                .format(java.time.format.DateTimeFormatter
                                                                                .ofPattern("dd/MM/yyyy HH:mm")));
                                                mov.put("tipoMovimiento", "INGRESO");
                                                mov.put("categoria", "Producto (Ecommerce)");
                                                mov.put("descripcion", "Pedido Web #" + pOnline.getId());
                                                mov.put("monto", totalPedido);
                                                movimientos.add(mov);

                                                totalIngresos += totalPedido;
                                        }
                                }

                                // ── EGRESOS: Gastos del Local (manuales, ej: luz, alquiler) ──
                                List<GastoLocal> gastos = gastoLocalRepository.findByFechaBetween(inicioDT, finDT);
                                for (GastoLocal g : gastos) {
                                        Map<String, Object> mov = new HashMap<>();
                                        mov.put("fechaOrden", g.getFecha());
                                        mov.put("fecha", g.getFecha().format(java.time.format.DateTimeFormatter
                                                        .ofPattern("dd/MM/yyyy HH:mm")));
                                        mov.put("tipoMovimiento", "EGRESO");
                                        mov.put("categoria", "Gasto del Local");
                                        mov.put("descripcion", g.getDescripcion());
                                        mov.put("monto", g.getMonto());
                                        movimientos.add(mov);

                                        totalEgresos += g.getMonto();
                                }

                                // ── EGRESOS: Abastecimiento de Inventario (Compras a Proveedores / Directas)
                                // ──
                                List<CompraProveedor> compras = compraProveedorRepository
                                                .findByFechaCompraBetween(inicioDT, finDT);
                                for (CompraProveedor c : compras) {
                                        Map<String, Object> mov = new HashMap<>();
                                        mov.put("fechaOrden", c.getFechaCompra());
                                        mov.put("fecha", c.getFechaCompra() != null
                                                        ? c.getFechaCompra()
                                                                        .format(java.time.format.DateTimeFormatter
                                                                                        .ofPattern("dd/MM/yyyy HH:mm"))
                                                        : "---");
                                        mov.put("tipoMovimiento", "EGRESO");
                                        mov.put("categoria", "Abastecimiento de Inventario");
                                        String nombreProd = c.getProducto() != null ? c.getProducto().getNombre()
                                                        : "Producto";
                                        String provNombre = (c.getProveedor() != null
                                                        && c.getProveedor().getNombre() != null)
                                                                        ? c.getProveedor().getNombre()
                                                                        : "Compra Directa";
                                        mov.put("descripcion",
                                                        "Abastecimiento: " + nombreProd + " (" + provNombre + ")");
                                        mov.put("monto", c.getTotalInvertido());
                                        movimientos.add(mov);

                                        totalEgresos += c.getTotalInvertido();
                                }

                                // ── EGRESOS: Pagos a Barberos (sueldos por comisión) ──
                                List<PagoBarbero> pagos = pbarberoRepository.findByFechaPagoBetween(inicioDT, finDT);
                                for (PagoBarbero p : pagos) {
                                        Map<String, Object> mov = new HashMap<>();
                                        mov.put("fechaOrden", p.getFechaPago());
                                        mov.put("fecha", p.getFechaPago().format(java.time.format.DateTimeFormatter
                                                        .ofPattern("dd/MM/yyyy HH:mm")));
                                        mov.put("tipoMovimiento", "EGRESO");
                                        String nombreBarbero = p.getBarbero() != null ? p.getBarbero().getNombre()
                                                        : "Barbero";
                                        mov.put("categoria", "Pago a Barbero");
                                        mov.put("descripcion", "Pago a " + nombreBarbero + " (" + p.getTipoPago() + ")"
                                                        + (p.getDescripcion() != null ? " - " + p.getDescripcion()
                                                                        : ""));
                                        mov.put("monto", p.getMontoPagado().doubleValue());
                                        movimientos.add(mov);

                                        totalEgresos += p.getMontoPagado().doubleValue();
                                }

                                // Orden cronológico descendente (más reciente primero)
                                movimientos.sort((a, b) -> {
                                        Comparable fa = (Comparable) a.get("fechaOrden");
                                        Comparable fb = (Comparable) b.get("fechaOrden");
                                        if (fa == null || fb == null)
                                                return 0;
                                        return fb.compareTo(fa);
                                });

                                totalNeto = totalIngresos - totalEgresos;
                                totalFiltrado = totalIngresos; // el KPI "Total Recaudado" sigue mostrando solo ingresos
                                model.addAttribute("movimientos", movimientos);
                        }
                }

                // Parámetros de búsqueda devueltos a la vista
                model.addAttribute("tipoOperacion", tipoOperacion);
                model.addAttribute("fechaInicio", inicio);
                model.addAttribute("fechaFin", fin);
                model.addAttribute("origen", origen);
                model.addAttribute("metodoPago", metodoPago);

                // Atributos de montos y KPIs para renderizar
                model.addAttribute("totalEfectivo", totalEfectivo);
                model.addAttribute("totalYape", totalYape);
                model.addAttribute("totalMixto", totalMixto);
                model.addAttribute("totalFiltrado", totalFiltrado);

                // Nuevos modelos agregados
                model.addAttribute("totalIngresos", totalIngresos);
                model.addAttribute("totalEgresos", totalEgresos);
                model.addAttribute("totalNeto", totalNeto);

                model.addAttribute("citasFiltradas", citasFiltradas);
                model.addAttribute("productosFiltrados", productosFiltrados);

                return "admin/panel-reportes";
        }

        // Endpoint para registrar egresos manuales desde el panel
        @PostMapping("/gasto")
        public String agregarGasto(
                        @RequestParam("descripcion") String descripcion,
                        @RequestParam("monto") double monto,
                        @RequestParam(name = "tipoOperacion", required = false) String tipoOperacion,
                        @RequestParam(name = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                        @RequestParam(name = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
                        @RequestParam(name = "origen", required = false, defaultValue = "TODOS") String origen,
                        @RequestParam(name = "metodoPago", required = false, defaultValue = "TODOS") String metodoPago) {

                GastoLocal nuevoGasto = new GastoLocal();
                nuevoGasto.setDescripcion(descripcion);
                nuevoGasto.setMonto(monto);
                nuevoGasto.setFecha(LocalDateTime.now());
                gastoLocalRepository.save(nuevoGasto);

                // Redirige de vuelta manteniendo los parámetros de búsqueda del usuario
                StringBuilder redirect = new StringBuilder("redirect:/admin/reportes?tipoOperacion=" + tipoOperacion);
                if (fechaInicio != null)
                        redirect.append("&fechaInicio=").append(fechaInicio);
                if (fechaFin != null)
                        redirect.append("&fechaFin=").append(fechaFin);
                redirect.append("&origen=").append(origen).append("&metodoPago=").append(metodoPago);

                return redirect.toString();
        }
}