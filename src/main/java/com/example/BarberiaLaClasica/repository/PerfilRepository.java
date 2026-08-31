package com.example.BarberiaLaClasica.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.BarberiaLaClasica.model.Perfil;

@Repository
public interface PerfilRepository extends JpaRepository<Perfil, Long> {
    Optional<Perfil> findByNombrePerfil(String nombrePerfil);
}