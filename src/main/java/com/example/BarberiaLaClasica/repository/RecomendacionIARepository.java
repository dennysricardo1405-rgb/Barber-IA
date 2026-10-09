package com.example.BarberiaLaClasica.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.BarberiaLaClasica.model.RecomendacionIA;

public interface RecomendacionIARepository extends JpaRepository<RecomendacionIA, Long> {

    Optional<RecomendacionIA> findFirstByClienteIdAndAprobadaTrueOrderByFechaRegistroDesc(Long clienteId);
}
