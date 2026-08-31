package com.example.BarberiaLaClasica.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.BarberiaLaClasica.model.Permiso;

@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Long> {
    Optional<Permiso> findByNombrePermiso(String nombrePermiso);
}