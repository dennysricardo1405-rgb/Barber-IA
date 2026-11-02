package com.example.BarberiaLaClasica.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.BarberiaLaClasica.model.BotConversacion;

public interface BotConversacionRepository extends JpaRepository<BotConversacion, Long> {

    Optional<BotConversacion> findFirstByChatIdOrderByUltimaActividadDesc(String chatId);

    List<BotConversacion> findTop20ByOrderByInicioDesc();
}
