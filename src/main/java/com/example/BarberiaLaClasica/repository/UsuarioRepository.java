package com.example.BarberiaLaClasica.repository;

import com.example.BarberiaLaClasica.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("SELECT COUNT(u) > 0 FROM Usuario u WHERE u.email = :email")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT u FROM Usuario u WHERE u.estado = :estado")
    List<Usuario> findByEstado(@Param("estado") Integer estado);

    @Query("SELECT u FROM Usuario u WHERE u.email = :email")
    Optional<Usuario> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM Usuario u WHERE u.perfil.id = :perfilId")
    List<Usuario> findByPerfilId(@Param("perfilId") Long perfilId);

    // ✅ NUEVO: contar usuarios por estado
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.estado = :estado")
    long countByEstado(@Param("estado") Integer estado);
}