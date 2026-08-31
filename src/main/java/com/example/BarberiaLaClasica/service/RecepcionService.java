package com.example.BarberiaLaClasica.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.BarberiaLaClasica.model.Barbero;
import com.example.BarberiaLaClasica.model.Cita;
import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.model.ConsumoSilla;
import com.example.BarberiaLaClasica.model.DetalleNotaVenta;
import com.example.BarberiaLaClasica.model.NotaVenta;
import com.example.BarberiaLaClasica.model.Producto;
import com.example.BarberiaLaClasica.model.Servicio;
import com.example.BarberiaLaClasica.model.SillaSession;
import com.example.BarberiaLaClasica.model.HistorialInventario; // ← Agregamos la entidad del Kardex
import com.example.BarberiaLaClasica.repository.BarberoRepository;
import com.example.BarberiaLaClasica.repository.CitaRepository;
import com.example.BarberiaLaClasica.repository.ClienteRepository;
import com.example.BarberiaLaClasica.repository.ConsumoSillaRepository;
import com.example.BarberiaLaClasica.repository.NotaVentaRepository;
import com.example.BarberiaLaClasica.repository.ProductoRepository;
import com.example.BarberiaLaClasica.repository.ServicioRepository;
import com.example.BarberiaLaClasica.repository.SillaSessionRepository;
import com.example.BarberiaLaClasica.repository.HistorialInventarioRepository; // ← Agregamos el repositorio del Kardex

@Service
public class RecepcionService {

    @Autowired
    private BarberoRepository barberoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ServicioRepository servicioRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private CitaRepository citaRepository;
    @Autowired
    private SillaSessionRepository sessionRepository;
    @Autowired
    private ConsumoSillaRepository consumoRepository;
    @Autowired
    private NotaVentaRepository notaVentaRepository;
    @Autowired
    private PromocionHelper promocionHelper;
    @Autowired
    private ClienteService clienteService;
    @Autowired
    private HistorialInventarioRepository historialInventarioRepository; // ← Inyectamos el Kardex aquí

    // ── Cita de reserva confirmada para hoy de un barbero ─────────────────────
    public Optional<Cita> getCitaReservaHoy(Long barberoId) {
        return citaRepository.findProximaCitaPorBarberoFechaEstado(
                barberoId, LocalDate.now(), 2);
    }

    // ── Sesión activa de un barbero ───────────────────────────────────────────
    public Optional<SillaSession> getSessionActiva(Long barberoId) {
        return sessionRepository.findByBarberoIdAndEstado(barberoId, 1);
    }

    public NotaVenta obtenerNota(Long id) {
        return notaVentaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nota no encontrada"));
    }

    // ── Listar todas las notas (mantener compatibilidad) ─────────────────────
    public List<NotaVenta> listarNotas() {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("fecha").descending());
        return notaVentaRepository.findAll(pageable).getContent();
    }

    // ── NUEVO: Listar notas con paginación ────────────────────────────────
    public Page<NotaVenta> listarNotasPaginadas(Pageable pageable) {
        return notaVentaRepository.findAll(pageable);
    }

    // ── Reservas Web para Hoy con Paginación (CORREGIDO) ─────────────────────
    public Page<Cita> listarReservasHoyPaginadas(Pageable pageable) {
        LocalDate hoy = LocalDate.now();
        return citaRepository.findByFechaAndEstadoOrderByHoraInicioAsc(hoy, 2, pageable);
    }

    // ── Abrir sesión desde RESERVA ────────────────────────────────────────────
    @Transactional
    public void atenderReserva(Long barberoId) {
        Cita cita = getCitaReservaHoy(barberoId)
                .orElseThrow(() -> new RuntimeException("No hay reserva confirmada para hoy"));

        Barbero barbero = cita.getBarbero();
        barbero.setEstado(2);
        barberoRepository.save(barbero);

        SillaSession session = new SillaSession();
        session.setBarbero(barbero);
        session.setCliente(cita.getCliente());
        session.setServicio(cita.getServicio());
        session.setCita(cita);
        sessionRepository.save(session);

        cita.setEstado(4);
        citaRepository.save(cita);
    }

    // ── Abrir sesión WALK-IN ─────────────────────────────────────────────────
    @Transactional
    public void ocuparSillaWalkin(Long barberoId, Long clienteId, Long servicioId) {
        getCitaReservaHoy(barberoId).ifPresent(cita -> {
            LocalTime ahora = LocalTime.now();
            LocalTime horaRes = cita.getHoraInicio();
            long minutos = java.time.Duration.between(ahora, horaRes).toMinutes();
            if (minutos >= 0 && minutos <= 30) {
                throw new RuntimeException(
                        "Hay una reserva en " + minutos + " min. " +
                                "Usa 'Atender Reserva' o espera a que pase.");
            }
        });

        Barbero barbero = barberoRepository.findById(barberoId)
                .orElseThrow(() -> new RuntimeException("Barbero no encontrado"));
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

        barbero.setEstado(2);
        barberoRepository.save(barbero);

        SillaSession session = new SillaSession();
        session.setBarbero(barbero);
        session.setServicio(servicio);
        if (clienteId != null)
            clienteRepository.findById(clienteId).ifPresent(session::setCliente);

        sessionRepository.save(session);
    }

    // ── Agregar producto ──────────────────────────────────────────────────────
    @Transactional
    public void agregarProducto(Long barberoId, Long productoId, int cantidad) {
        SillaSession session = getSessionActiva(barberoId)
                .orElseThrow(() -> new RuntimeException("No hay sesión activa"));

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        int cantidadExistente = consumoRepository.findBySessionId(session.getId()).stream()
                .filter(c -> c.getProducto() != null && c.getProducto().getId().equals(productoId))
                .mapToInt(ConsumoSilla::getCantidad)
                .sum();

        int cantidadTotalDeseada = cantidadExistente + cantidad;

        if (producto.getStock() < cantidadTotalDeseada) {
            if (cantidadExistente > 0) {
                throw new RuntimeException("Stock insuficiente: solo hay " + producto.getStock() + 
                        " en almacén y ya agregaste " + cantidadExistente + " a la silla.");
            } else {
                throw new RuntimeException("Stock insuficiente: solo hay " + producto.getStock() + " disponible.");
            }
        }

        consumoRepository.findBySessionId(session.getId()).stream()
                .filter(c -> c.getProducto() != null && c.getProducto().getId().equals(productoId))
                .findFirst()
                .ifPresentOrElse(c -> {
                    c.setCantidad(c.getCantidad() + cantidad);
                    c.setSubtotal(c.getCantidad() * producto.getPrecioVenta());
                    consumoRepository.save(c);
                }, () -> {
                    ConsumoSilla nuevo = new ConsumoSilla();
                    nuevo.setSession(session);
                    nuevo.setProducto(producto);
                    nuevo.setTipo("PRODUCTO");
                    nuevo.setCantidad(cantidad);
                    nuevo.setSubtotal(cantidad * producto.getPrecioVenta());
                    consumoRepository.save(nuevo);
                });
    }

    // ── Agregar servicio extra ───────────────────────────────────────────────
    @Transactional
    public void agregarServicio(Long barberoId, Long servicioId) {
        SillaSession session = getSessionActiva(barberoId)
                .orElseThrow(() -> new RuntimeException("No hay sesión activa"));

        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

        Cliente clienteAtencion = session.getCliente();
        if (clienteAtencion != null) {
            clienteAtencion.setTotalVisitas(clienteService.calcularTotalVisitas(clienteAtencion));
        }

        double precioRegular = servicio.getPrecio().doubleValue();

        ConsumoSilla nuevo = new ConsumoSilla();
        nuevo.setSession(session);
        nuevo.setServicio(servicio);
        nuevo.setTipo("SERVICIO");
        nuevo.setCantidad(1);
        nuevo.setSubtotal(precioRegular);
        consumoRepository.save(nuevo);
    }

    // ── Quitar consumo ────────────────────────────────────────────────────────
    @Transactional
    public void quitarConsumo(Long consumoId) {
        consumoRepository.deleteById(consumoId);
    }

    public List<ConsumoSilla> obtenerConsumos(Long barberoId) {
        return getSessionActiva(barberoId)
                .map(s -> consumoRepository.findBySessionId(s.getId()))
                .orElse(List.of());
    }

    @Transactional
    public NotaVenta finalizarAtencion(Long barberoId, String metodoPago, double montoYape, String codigoYape) {
        SillaSession session = getSessionActiva(barberoId)
                .orElseThrow(() -> new RuntimeException("No hay sesión activa"));

        session = sessionRepository.findByIdConRelaciones(session.getId())
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        List<ConsumoSilla> consumos = consumoRepository.findBySessionId(session.getId());

        NotaVenta nota = new NotaVenta();
        nota.setSession(session);
        nota.setBarbero(session.getBarbero());
        nota.setCliente(session.getCliente());

        nota.setMetodoPago(metodoPago);
        nota.setMontoYape(montoYape);

        List<DetalleNotaVenta> detalles = new ArrayList<>();

        com.example.BarberiaLaClasica.model.Cliente clienteAtencion = session.getCliente();
        if (clienteAtencion != null) {
            clienteAtencion.setTotalVisitas(clienteService.calcularTotalVisitas(clienteAtencion));
        }

        // ── 1. PRECIO DEL SERVICIO PRINCIPAL (Promoción solo si es Cita Web) ─────────
        double precioServicioFinal = session.getServicio().getPrecio().doubleValue();
        if (session.getCita() != null && session.getCita().getTotalPrecio() != null) {
            precioServicioFinal = session.getCita().getTotalPrecio().doubleValue();
        }

        DetalleNotaVenta linServicio = new DetalleNotaVenta();
        linServicio.setNotaVenta(nota);
        linServicio.setDescripcion(session.getServicio().getNombre());
        linServicio.setCantidad(1);
        linServicio.setPrecioUnitario(precioServicioFinal);
        linServicio.setSubtotal(precioServicioFinal);
        linServicio.setTipo("SERVICIO");
        detalles.add(linServicio);

        // ── 2. CONDICIONAL DE PROMOCIÓN Y REGISTRO DE CONSUMOS ────────────
        for (ConsumoSilla c : consumos) {
            if (c.getProducto() != null) {
                double precioProductoFinal = c.getProducto().getPrecioVenta();
                double subtotalProductoFinal = precioProductoFinal * c.getCantidad();

                DetalleNotaVenta lin = new DetalleNotaVenta();
                lin.setNotaVenta(nota);
                lin.setDescripcion(c.getProducto().getNombre());
                lin.setCantidad(c.getCantidad());
                lin.setPrecioUnitario(precioProductoFinal);
                lin.setSubtotal(subtotalProductoFinal);
                lin.setTipo("PRODUCTO");
                detalles.add(lin);

                // Descuento de Stock físico
                Producto p = c.getProducto();
                p.setStock(p.getStock() - c.getCantidad());
                productoRepository.save(p);

                // Kardex
                HistorialInventario movimiento = new HistorialInventario();
                movimiento.setProducto(p);
                movimiento.setTipoMovimiento("SALIDA");
                movimiento.setCantidad(c.getCantidad());
                movimiento.setStockResultante(p.getStock());
                movimiento.setMotivo("Venta en Caja - Atendido por Barbero: " + 
                        (session.getBarbero() != null ? session.getBarbero().getNombre() : "General"));
                
                historialInventarioRepository.save(movimiento);
            } else if (c.getServicio() != null) {
                double precioServicioExtra = c.getServicio().getPrecio().doubleValue();

                DetalleNotaVenta lin = new DetalleNotaVenta();
                lin.setNotaVenta(nota);
                lin.setDescripcion(c.getServicio().getNombre());
                lin.setCantidad(1);
                lin.setPrecioUnitario(precioServicioExtra);
                lin.setSubtotal(precioServicioExtra);
                lin.setTipo("SERVICIO");
                detalles.add(lin);
            }
        }

        // ── 3. Cálculo Final de Totales y Estructura Financiera ──────────
        double total = detalles.stream().mapToDouble(DetalleNotaVenta::getSubtotal).sum();
        nota.setTotal(total);

        double adelantoPrevio = 0.0;
        if (session.getCita() != null && session.getCita().getMontoYape() != null) {
            adelantoPrevio = session.getCita().getMontoYape().doubleValue();
        }

        double saldoPendienteEnSilla = Math.max(0, total - adelantoPrevio);

        if ("EFECTIVO".equalsIgnoreCase(metodoPago)) {
            nota.setMontoYape(adelantoPrevio);
            nota.setMontoEfectivo(saldoPendienteEnSilla);
        } else if ("YAPE".equalsIgnoreCase(metodoPago)) {
            double cobradoYapeEnSilla = (montoYape > 0 && montoYape <= total) ? montoYape : saldoPendienteEnSilla;
            nota.setMontoYape(adelantoPrevio + cobradoYapeEnSilla);
            nota.setMontoEfectivo(Math.max(0, total - nota.getMontoYape()));
            nota.setCodigoYape(codigoYape);
        } else {
            double cobradoYapeEnSilla = montoYape > 0 ? montoYape : saldoPendienteEnSilla;
            nota.setMontoYape(adelantoPrevio + cobradoYapeEnSilla);
            nota.setMontoEfectivo(Math.max(0, total - nota.getMontoYape()));
            nota.setCodigoYape(codigoYape);
        }

        nota.setDetalles(detalles);
        notaVentaRepository.save(nota);

        // ── 4. Actualización de Cita y Sesión ──────────────────────────────
        if (session.getCita() != null) {
            session.getCita().setEstado(3);
            citaRepository.save(session.getCita());
        }

        session.setEstado(0);
        sessionRepository.save(session);

        Barbero barbero = session.getBarbero();
        barbero.setEstado(1);
        barberoRepository.save(barbero);

        return nota;
    }

    @Transactional
    public void asociarCliente(Long barberoId, Long clienteId) {
        SillaSession session = sessionRepository
                .findByBarberoIdAndEstado(barberoId, 1)
                .orElseThrow(() -> new RuntimeException("No hay sesión activa"));

        clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        sessionRepository.actualizarCliente(session.getId(), clienteId);
    }

    public List<NotaVenta> listarNotasPorRango(java.time.LocalDateTime inicio, java.time.LocalDateTime fin) {
        return notaVentaRepository.findByFechaBetween(inicio, fin);
    }

    public Page<NotaVenta> listarNotasPorRangoPaginadas(java.time.LocalDateTime inicio, java.time.LocalDateTime fin, Pageable pageable) {
        return notaVentaRepository.findByFechaBetween(inicio, fin, pageable);
    }

    // ── PROCESAR VENTA DIRECTA DE PRODUCTOS (RECEPCIÓN / CAJA) ─────────────────
    @Transactional
    public NotaVenta procesarVentaDirectaProductos(
            Long clienteId,
            List<Map<String, Object>> items,
            String metodoPago,
            double montoEfectivo,
            double montoYape,
            String codigoYape) {

        NotaVenta nota = new NotaVenta();
        if (clienteId != null && clienteId > 0) {
            Cliente c = clienteRepository.findById(clienteId).orElse(null);
            nota.setCliente(c);
            if (c != null) {
                c.setTotalVisitas(clienteService.calcularTotalVisitas(c));
            }
        }

        nota.setMetodoPago(metodoPago != null ? metodoPago : "EFECTIVO");
        nota.setMontoEfectivo(montoEfectivo);
        nota.setMontoYape(montoYape);
        nota.setCodigoYape(codigoYape);

        double totalNota = 0.0;
        List<DetalleNotaVenta> detalles = new ArrayList<>();

        for (Map<String, Object> item : items) {
            Long productoId = Long.valueOf(item.get("productoId").toString());
            int cantidad = Integer.parseInt(item.get("cantidad").toString());

            Producto p = productoRepository.findById(productoId)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + productoId));

            if (p.getStock() < cantidad) {
                throw new RuntimeException("Stock insuficiente para: " + p.getNombre() + " (Stock actual: " + p.getStock() + ")");
            }

            double precioUnitario = p.getPrecioVenta();
            double subtotal = precioUnitario * cantidad;
            totalNota += subtotal;

            // Descuento de stock
            p.setStock(p.getStock() - cantidad);
            productoRepository.save(p);

            // Registro de Kardex
            HistorialInventario kardex = new HistorialInventario();
            kardex.setProducto(p);
            kardex.setTipoMovimiento("SALIDA");
            kardex.setCantidad(cantidad);
            kardex.setStockResultante(p.getStock());
            kardex.setMotivo("Venta Directa de Producto en Recepción");
            historialInventarioRepository.save(kardex);

            // Detalle de la nota de venta
            DetalleNotaVenta det = new DetalleNotaVenta();
            det.setNotaVenta(nota);
            det.setDescripcion(p.getNombre());
            det.setCantidad(cantidad);
            det.setPrecioUnitario(precioUnitario);
            det.setSubtotal(subtotal);
            det.setTipo("PRODUCTO");
            detalles.add(det);
        }

        nota.setTotal(totalNota);
        nota.setDetalles(detalles);

        return notaVentaRepository.save(nota);
    }
}