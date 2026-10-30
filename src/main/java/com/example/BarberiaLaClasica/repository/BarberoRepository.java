package com.example.BarberiaLaClasica.repository;

import com.example.BarberiaLaClasica.model.Barbero;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BarberoRepository extends JpaRepository<Barbero, Long> {

    List<Barbero> findByEstado(int estado);

    Optional<Barbero> findByUsuarioEmail(String email);

    // Métodos para paginación
    Page<Barbero> findAll(Pageable pageable);

    Page<Barbero> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    // Búsqueda avanzada en múltiples campos
    Page<Barbero> findByNombreContainingIgnoreCaseOrEspecialidadContainingIgnoreCaseOrTelefonoContainingIgnoreCase(
            String nombre, String especialidad, String telefono, Pageable pageable);
}