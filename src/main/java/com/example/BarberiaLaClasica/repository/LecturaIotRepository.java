package com.example.BarberiaLaClasica.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.BarberiaLaClasica.model.LecturaIot;

public interface LecturaIotRepository extends JpaRepository<LecturaIot, Long> {

    List<LecturaIot> findByTipoAndFechaGreaterThanEqualOrderByFechaAsc(String tipo, LocalDateTime desde);

    List<LecturaIot> findTop60ByTipoOrderByFechaDesc(String tipo);
}
