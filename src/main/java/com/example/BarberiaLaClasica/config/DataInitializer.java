package com.example.BarberiaLaClasica.config;

import jakarta.transaction.Transactional;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.BarberiaLaClasica.model.Perfil;
import com.example.BarberiaLaClasica.model.Permiso;
import com.example.BarberiaLaClasica.model.Usuario;
import com.example.BarberiaLaClasica.repository.PerfilRepository;
import com.example.BarberiaLaClasica.repository.PermisoRepository;
import com.example.BarberiaLaClasica.repository.UsuarioRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private PermisoRepository permisoRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        try {
            jdbcTemplate.execute("ALTER TABLE consumos_silla MODIFY COLUMN producto_id BIGINT NULL");
        } catch (Exception ignored) {
        }

        // 1. Crear la lista completa de permisos basados en tu base de datos local
        crearPermisoSiNoExiste("GESTION_USUARIOS", "Gestión de Personal", "Administrar usuarios");
        crearPermisoSiNoExiste("GESTION_CLIENTES", "Gestión de Clientes", "Registrar y gestionar clientes");
        crearPermisoSiNoExiste("GESTION_CITAS", "Gestión de Citas", "Ver y gestionar citas");
        crearPermisoSiNoExiste("GESTION_SERVICIOS", "Planes y Servicios", "Administrar planes de barbería");
        crearPermisoSiNoExiste("GESTION_PRODUCTOS", "Gestión de Productos", "Administrar productos e inventario");
        crearPermisoSiNoExiste("GESTION_REPORTES", "Reportes", "Ver reportes e ingresos");
        crearPermisoSiNoExiste("GESTION_CATEGORIAS", "Categorias", "Gestionar Categorias de los productos");
        crearPermisoSiNoExiste("GESTION_PROVEDORES", "Provedores", "Gestionar los provedores");
        crearPermisoSiNoExiste("GESTION_INVENTARIO", "INVENTARIO", "Gestionar el stock de los productos");

        // 2. Crear perfiles si no existen (Buscando por nombre para evitar problemas de
        // ID)
        Perfil adminPerfil = crearPerfilSiNoExiste("Administrador", "Acceso total");
        crearPerfilSiNoExiste("Secretario", "Gestión de citas y clientes");

        // 3. Asignar todos los permisos al admin si no tiene ninguno
        if (adminPerfil.getPermisos() == null || adminPerfil.getPermisos().isEmpty()) {
            List<Permiso> todosLosPermisos = permisoRepository.findAll();
            adminPerfil.setPermisos(todosLosPermisos);
            adminPerfil = perfilRepository.save(adminPerfil);
        }

        // 4. Crear usuario admin si no existe
        if (!usuarioRepository.existsByEmail("admin@gmail.com")) {
            Usuario admin = new Usuario();
            admin.setNombre("Administrador");
            admin.setEmail("admin@gmail.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEstado(1);
            admin.setPerfil(adminPerfil);
            usuarioRepository.save(admin);
            System.out.println("✅ Usuario admin creado con éxito.");
        } else {
            System.out.println("ℹ️ Admin ya existe.");
        }
    }

    private Perfil crearPerfilSiNoExiste(String nombre, String descripcion) {
        // Nota: Asegúrate de tener el método findByNombrePerfil en tu PerfilRepository
        return perfilRepository.findByNombrePerfil(nombre).orElseGet(() -> {
            Perfil p = new Perfil();
            p.setNombrePerfil(nombre);
            p.setDescripcion(descripcion);
            System.out.println("✅ Perfil creado: " + nombre);
            return perfilRepository.save(p);
        });
    }

    private void crearPermisoSiNoExiste(String nombrePermiso, String nombre, String descripcion) {
        permisoRepository.findByNombrePermiso(nombrePermiso).orElseGet(() -> {
            Permiso p = new Permiso();
            p.setNombrePermiso(nombrePermiso);
            p.setNombre(nombre);
            p.setDescripcion(descripcion);
            System.out.println("Permiso creado: " + nombrePermiso);
            return permisoRepository.save(p);
        });
    }
}