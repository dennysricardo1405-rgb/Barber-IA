package com.example.BarberiaLaClasica.service;

import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.model.Usuario;
import com.example.BarberiaLaClasica.repository.ClienteRepository;
import com.example.BarberiaLaClasica.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // 1. Buscar primero en usuarios (admin/secretario)
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();

            if (usuario.getEstado() == 0) {
                throw new UsernameNotFoundException("Usuario inactivo: " + email);
            }

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            // Rol principal
            String rol = "ROLE_" + normalizarRol(usuario.getPerfil().getNombrePerfil());
            authorities.add(new SimpleGrantedAuthority(rol));

            // Permisos del perfil
            usuario.getPerfil().getPermisos()
                    .forEach(permiso -> authorities.add(new SimpleGrantedAuthority(permiso.getNombrePermiso())));

            return new User(usuario.getEmail(), usuario.getPassword(), authorities);
        }

        // 2. Si no está en usuarios, buscar en clientes
        Optional<Cliente> clienteOpt = clienteRepository.findByCorreo(email);
        if (clienteOpt.isPresent()) {
            Cliente cliente = clienteOpt.get();

            if (cliente.getEstado() == 0) {
                throw new UsernameNotFoundException("Cliente inactivo: " + email);
            }

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_CLIENTE"));

            return new User(cliente.getCorreo(), cliente.getPassword(), authorities);
        }

        throw new UsernameNotFoundException("No se encontró cuenta con el email: " + email);
    }

    private String normalizarRol(String nombrePerfil) {
        return nombrePerfil
                .toUpperCase()
                .replace("Ó", "O")
                .replace("É", "E")
                .replace("Á", "A")
                .replace("Í", "I")
                .replace("Ú", "U")
                .replace(" ", "_");
    }
}