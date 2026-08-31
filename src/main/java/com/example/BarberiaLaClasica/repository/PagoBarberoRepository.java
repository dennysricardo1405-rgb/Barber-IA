package com.example.BarberiaLaClasica.repository;

import com.example.BarberiaLaClasica.model.PagoBarbero;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface PagoBarberoRepository extends JpaRepository<PagoBarbero, Long> {
    List<PagoBarbero> findByFechaPagoBetween(LocalDateTime inicio, LocalDateTime fin);
}