package com.example.BarberiaLaClasica.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.BarberiaLaClasica.model.CorteReferencia;

public interface CorteReferenciaRepository extends JpaRepository<CorteReferencia, Long> {

    List<CorteReferencia> findAllByOrderByNombreAsc();

    List<CorteReferencia> findByActivoTrueOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
