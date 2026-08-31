package com.example.BarberiaLaClasica.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.BarberiaLaClasica.model.ConsumoSilla;

public interface ConsumoSillaRepository extends JpaRepository<ConsumoSilla, Long> {
    List<ConsumoSilla> findBySessionId(Long sessionId);
    void deleteBySessionId(Long sessionId);
}
