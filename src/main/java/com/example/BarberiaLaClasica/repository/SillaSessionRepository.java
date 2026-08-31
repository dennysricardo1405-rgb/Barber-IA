package com.example.BarberiaLaClasica.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.BarberiaLaClasica.model.SillaSession;

public interface SillaSessionRepository extends JpaRepository<SillaSession, Long> {

    Optional<SillaSession> findByBarberoIdAndEstado(Long barberoId, int estado);

    @Query("SELECT s FROM SillaSession s " +
            "LEFT JOIN FETCH s.cliente " +
            "LEFT JOIN FETCH s.barbero " +
            "LEFT JOIN FETCH s.servicio " +
            "WHERE s.id = :id")
    Optional<SillaSession> findByIdConRelaciones(@Param("id") Long id);

    // ← NUEVO: update directo sin pasar por caché de Hibernate
    @Modifying
    @Query("UPDATE SillaSession s SET s.cliente.id = :clienteId WHERE s.id = :sessionId")
    void actualizarCliente(@Param("sessionId") Long sessionId, @Param("clienteId") Long clienteId);
}