package com.example.BarberiaLaClasica.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.example.BarberiaLaClasica.service.UserDetailsServiceImpl;

import jakarta.servlet.http.HttpSession;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/secretario/recepcion/api-consumos/**",
                                "/api/clientes/**"))
                .userDetailsService(userDetailsService)
                .authorizeHttpRequests(auth -> auth
                        // 1. ✅ Rutas públicas y recursos estáticos
                        .requestMatchers(
                                "/", "/cliente/login", "/cliente/registro", "/reservar",
                                "/api/citas/horas-disponibles",
                                "/api/citas/pre-reserva",
                                "/api/clientes/consulta-dni/**", "/api/citas/pre-reserva", "/acceso-denegado",
                                "/css/**", "/js/**", "/images/**", "/uploads/**")
                        .permitAll()

                        // 2. ✅ APIs internas compartidas
                        .requestMatchers("/api/clientes/**").hasAnyRole("ADMINISTRADOR", "SECRETARIO")
                        .requestMatchers("/api/ventas-productos/**", "/secretario/ventas-productos/**", "/secretario/api/ventas-productos/**").hasAnyRole("ADMINISTRADOR", "SECRETARIO")

                        // 3. 🆕 Mantenimiento compartido de clientes
                        .requestMatchers("/admin/cliente/**", "/admin/cliente")
                        .hasAnyRole("ADMINISTRADOR", "SECRETARIO")

                        // 4. 🔒 CONTROL DE PRODUCTOS: Permite al secretario consumir la data para las
                        // mesas/sillas
                        // pero restringe acciones críticas de edición/eliminación si entran por la ruta
                        // de admin
                        .requestMatchers("/admin/productos/compras/**").hasAnyRole("ADMINISTRADOR", "SECRETARIO")
                        .requestMatchers("/admin/productos").hasAnyRole("ADMINISTRADOR", "SECRETARIO") // Ver catálogo

                        // 5. ✅ Secretario (Su panel exclusivo de recepción en vivo)
                        .requestMatchers("/secretario/**").hasRole("SECRETARIO")

                        // 6. ✅ Área del cliente web
                        .requestMatchers("/cliente/dashboard/**", "/cliente/perfil/**").hasRole("CLIENTE")
                        .requestMatchers("/api/carrito/**").permitAll()
                        .requestMatchers("/cliente/carrito/**", "/cliente/mis-pedidos").hasRole("CLIENTE")
                        .requestMatchers("/secretario/pedidos/**").hasAnyRole("SECRETARIO", "ADMINISTRADOR")
                        .requestMatchers("/api/carrito/**").permitAll()

                        .requestMatchers("/api/auth/check").permitAll()
                        // 7. 🚫 CANDADO GENERAL ADMIN: Todo lo demás (CRUD de productos, barberos,
                        // reportes) es estricto del ADMIN
                        .requestMatchers("/admin/**").hasRole("ADMINISTRADOR")

                        // 8. 🔒 Requisito de autenticación base para cualquier otra esquina
                        .anyRequest().authenticated())

                .formLogin(form -> form
                        .loginPage("/cliente/login")
                        .loginProcessingUrl("/cliente/login")
                        .successHandler((request, response, authentication) -> {
                            String role = authentication.getAuthorities().stream()
                                    .map(a -> a.getAuthority())
                                    .filter(a -> a.startsWith("ROLE_"))
                                    .findFirst().orElse("");

                            if (role.equals("ROLE_ADMINISTRADOR")) {
                                response.sendRedirect("/admin/dashboard");
                            } else if (role.equals("ROLE_SECRETARIO")) {
                                response.sendRedirect("/secretario/dashboard");
                            } else if (role.equals("ROLE_CLIENTE")) {
                                // Si venía del flujo de reserva, manda al pago
                                HttpSession session = request.getSession(false);
                                Object preCita = session != null ? session.getAttribute("preCita_servicioId") : null;
                                if (preCita != null) {
                                    response.sendRedirect("/cliente/reserva/pago");
                                } else {
                                    response.sendRedirect("/");
                                }
                            } else {
                                response.sendRedirect("/");
                            }
                        })
                        .failureUrl("/cliente/login?error")
                        .permitAll())

                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/cliente/login?accesoDenegado"))

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/cliente/login?logout")
                        .permitAll());

        return http.build();
    }
}