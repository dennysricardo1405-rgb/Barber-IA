package com.example.BarberiaLaClasica.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.BarberiaLaClasica.model.Perfil;
import com.example.BarberiaLaClasica.model.Producto;
import com.example.BarberiaLaClasica.model.Usuario;
import com.example.BarberiaLaClasica.repository.BarberoRepository;
import com.example.BarberiaLaClasica.repository.CategoriaRepository;
import com.example.BarberiaLaClasica.repository.ClienteRepository;
import com.example.BarberiaLaClasica.repository.DetalleNotaVentaRepository;
import com.example.BarberiaLaClasica.repository.NotaVentaRepository;
import com.example.BarberiaLaClasica.repository.PedidoOnlineRepository;
import com.example.BarberiaLaClasica.repository.PerfilRepository;
import com.example.BarberiaLaClasica.repository.ProductoRepository;
import com.example.BarberiaLaClasica.repository.ServicioRepository;
import com.example.BarberiaLaClasica.service.BarberoService;
import com.example.BarberiaLaClasica.service.ConfiguracionSitioService;
import com.example.BarberiaLaClasica.service.PerfilService;
import com.example.BarberiaLaClasica.service.PromocionHelper;
import com.example.BarberiaLaClasica.service.PromocionService;
import com.example.BarberiaLaClasica.service.SliderImageService;
import com.example.BarberiaLaClasica.service.UsuarioService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class NavigationController {

    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private PerfilRepository perfilRepository;
    @Autowired
    private PerfilService perfilService;
    @Autowired
    private BarberoRepository barberoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private BarberoService barberoService;
    @Autowired
    private ServicioRepository servicioRepository;
    @Autowired
    private SliderImageService sliderImageService;
    @Autowired
    private PromocionService promocionService;
    @Autowired
    private ConfiguracionSitioService configuracionSitioService;
    @Autowired
    private PromocionHelper promocionHelper;
    @Autowired
    private NotaVentaRepository notaVentaRepository;
    @Autowired
    private DetalleNotaVentaRepository detalleNotaVentaRepository;
    @Autowired
    private PedidoOnlineRepository pedidoOnlineRepository;
    @Autowired
    private com.example.BarberiaLaClasica.service.ClienteService clienteService;

    @GetMapping("/")
    public String index(Model model, java.security.Principal principal) {
        List<Producto> productosWeb = productoRepository.findProductosParaCatalogoPublico("Productos de Barbería");

        com.example.BarberiaLaClasica.model.Cliente clienteLogueado = null;
        if (principal != null) {
            clienteLogueado = clienteRepository.findByCorreo(principal.getName()).orElse(null);
            if (clienteLogueado != null) {
                clienteLogueado.setTotalVisitas(clienteService.calcularTotalVisitas(clienteLogueado));
            }
        }

        model.addAttribute("clienteLogueado", clienteLogueado);
        model.addAttribute("productosBarberia", productosWeb);
        model.addAttribute("servicios", servicioRepository.findByEstado(1));
        model.addAttribute("barberos", barberoService.listarTodos());
        model.addAttribute("subcategoriasBarberia",
                categoriaRepository.findByPadreNombreAndActivoTrue("Productos de Barbería"));
        model.addAttribute("sliderImagenes", sliderImageService.listarActivas());
        model.addAttribute("promociones", promocionService.listarActivas());
        model.addAttribute("config", configuracionSitioService.obtenerMapa());
        model.addAttribute("serviciosDestacados", servicioRepository.findByEstado(1));
        model.addAttribute("promoHelper", promocionHelper);

        return "index";
    }

    // ── Dashboard Admin ──────────────────────────────────────
    @GetMapping("/admin/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        model.addAttribute("usuarioNombre", authentication.getName());

        // 1. Contadores básicos de personal y clientes
        long totalBarberos = barberoRepository.count();
        long totalClientes = clienteRepository.count();
        model.addAttribute("totalBarberos", totalBarberos);
        model.addAttribute("totalClientes", totalClientes);

        // 2. Conteo dinámico de existencias en peligro (Límite <= 2 unidades)
        long stockBajo = productoRepository.countByStockLessThanEqual(2);
        model.addAttribute("productosStockBajo", stockBajo);

        // 3. Cálculo de Ingresos Totales acumulados del mes corriente
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate finMes = LocalDate.now();

        // Extraemos todas las notas de venta del mes para totalizar la recaudación
        double ingresosPresencial = detalleNotaVentaRepository
                .findByFechaBetween(inicioMes.atStartOfDay(), LocalDate.now().atTime(23, 59, 59)).stream()
                .mapToDouble(d -> d.getSubtotal())
                .sum();

        // 2. Sumar los pedidos online (Web) aceptados del mes
        double ingresosWeb = pedidoOnlineRepository.findAll().stream()
                .filter(p -> (p.getEstado() == 2 || p.getEstado() == 3) && p.getFechaPedido() != null)
                .filter(p -> !p.getFechaPedido().toLocalDate().isBefore(inicioMes)
                        && !p.getFechaPedido().toLocalDate().isAfter(finMes))
                .mapToDouble(p -> p.getTotal())
                .sum();

        double totalMensual = ingresosPresencial + ingresosWeb;
        model.addAttribute("ingresosMes", String.format("%.2f", totalMensual));

        model.addAttribute("ingresosMes", String.format("%.2f", totalMensual));
        model.addAttribute("citasHoy", 0); // Mantener temporalmente en 0 si aún no tienes el flujo de citas del día

        return "admin-dashboard";
    }

    // ── Usuarios ─────────────────────────────────────────────
    @GetMapping("/admin/usuarios")
    public String gestionUsuarios(Model model, Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Usuario> usuariosPage = usuarioService.listarTodosPaginado(pageable);

        // ← Solo perfiles que NO sean Administrador para crear nuevos usuarios
        List<Perfil> perfilesSinAdmin = perfilService.listarTodo().stream()
                .filter(p -> !p.getNombrePerfil().equalsIgnoreCase("Administrador"))
                .toList();

        model.addAttribute("usuariosPage", usuariosPage);
        model.addAttribute("usuarios", usuariosPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", usuariosPage.getTotalPages());
        model.addAttribute("totalItems", usuariosPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("perfiles", perfilService.listarTodo()); // ← para modal editar (todos)
        model.addAttribute("perfilesSinAdmin", perfilesSinAdmin); // ← para modal nuevo (sin admin)
        model.addAttribute("usuarioLogueado", authentication.getName());
        return "usuarios-lista";
    }

    @GetMapping("/admin/usuarios/estado/{id}")
    public String cambiarEstadoUsuario(@PathVariable("id") Long id) {
        usuarioService.cambiarEstado(id);
        return "redirect:/admin/usuarios";
    }

    // ── Contador de usuarios activos (badge y barra) ─────────
    @GetMapping("/admin/usuarios/count")
    @ResponseBody
    public Map<String, Long> contarUsuarios() {
        return Map.of("total", usuarioService.contarActivos());
    }

    @PostMapping("/admin/usuarios/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario,
            @RequestParam("perfilId") Long perfilId,
            Model model, Authentication authentication) {
        try {
            Perfil p = perfilRepository.findById(perfilId)
                    .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));
            usuario.setPerfil(p);
            usuarioService.guardar(usuario);
            return "redirect:/admin/usuarios";
        } catch (IllegalStateException e) {
            model.addAttribute("errorLimite", "⚠️ No puedes agregar más usuarios. El máximo permitido es 5.");
            return cargarModeloUsuarios(model, authentication);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorLimite", "⚠️ El email ya está registrado. Usa uno diferente.");
            return cargarModeloUsuarios(model, authentication);
        } catch (Exception e) {
            model.addAttribute("errorLimite", "⚠️ Ocurrió un error al guardar. Verifica los datos e intenta de nuevo.");
            return cargarModeloUsuarios(model, authentication);
        }
    }

    @PostMapping("/admin/usuarios/editar")
    public String editarUsuario(@ModelAttribute Usuario usuario,
            @RequestParam("perfilId") Long perfilId,
            Model model, Authentication authentication) {
        try {
            Perfil p = perfilRepository.findById(perfilId)
                    .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));
            usuario.setPerfil(p);
            usuarioService.editarUsuario(usuario);
            return "redirect:/admin/usuarios?editado";
        } catch (Exception e) {
            model.addAttribute("errorLimite", "⚠️ Error al editar. Verifica que el email no esté duplicado.");
            return cargarModeloUsuarios(model, authentication);
        }
    }

    @GetMapping("/admin/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable("id") Long id) {
        usuarioService.eliminarLogico(id);
        return "redirect:/admin/usuarios?eliminado";
    }

    // ── Método auxiliar para recargar modelo en caso de error ─
    private String cargarModeloUsuarios(Model model, Authentication authentication) {
        Pageable pageable = PageRequest.of(0, 5, Sort.by("id").ascending());
        Page<Usuario> usuariosPage = usuarioService.listarTodosPaginado(pageable);

        List<Perfil> perfilesSinAdmin = perfilService.listarTodo().stream()
                .filter(p -> !p.getNombrePerfil().equalsIgnoreCase("Administrador"))
                .toList();

        model.addAttribute("usuariosPage", usuariosPage);
        model.addAttribute("usuarios", usuariosPage.getContent());
        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", usuariosPage.getTotalPages());
        model.addAttribute("totalItems", usuariosPage.getTotalElements());
        model.addAttribute("size", 5);
        model.addAttribute("perfiles", perfilService.listarTodo());
        model.addAttribute("perfilesSinAdmin", perfilesSinAdmin);
        model.addAttribute("usuarioLogueado", authentication.getName());
        return "usuarios-lista";
    }
}