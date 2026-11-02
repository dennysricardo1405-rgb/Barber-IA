package com.example.BarberiaLaClasica.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.BarberiaLaClasica.model.LineaBaseManual;

public interface LineaBaseManualRepository extends JpaRepository<LineaBaseManual, Long> {

    List<LineaBaseManual> findAllByOrderByFechaDescIdDesc();
}
