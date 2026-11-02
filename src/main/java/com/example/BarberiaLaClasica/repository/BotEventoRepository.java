package com.example.BarberiaLaClasica.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.BarberiaLaClasica.model.BotEvento;

public interface BotEventoRepository extends JpaRepository<BotEvento, Long> {

    List<BotEvento> findTop20ByOrderByFechaDesc();

    long countByTipo(String tipo);

    List<BotEvento> findByTipo(String tipo);
}
