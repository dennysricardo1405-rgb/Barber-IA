package com.example.BarberiaLaClasica.repository;

import com.example.BarberiaLaClasica.model.Barbero;
import com.example.BarberiaLaClasica.model.Cita;
import com.example.BarberiaLaClasica.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

        // Historial de citas de un cliente ordenado por fecha descendente
        List<Cita> findByClienteOrderByFechaDescHoraInicioDesc(Cliente cliente);

        // Contar citas de un cliente por estados (ej: atendida/completada)
        long countByClienteAndEstadoIn(Cliente cliente, List<Integer> estados);

        // Verificar si un horario ya está ocupado para un barbero
        @Query("""
                            SELECT COUNT(c) > 0 FROM Cita c
                            WHERE c.barbero.id = :barberoId
                            AND c.fecha = :fecha
                            AND c.estado NOT IN (0)
                            AND (
                                (c.horaInicio <= :horaInicio AND c.horaFin > :horaInicio)
                                OR (c.horaInicio < :horaFin AND c.horaFin >= :horaFin)
                                OR (c.horaInicio >= :horaInicio AND c.horaFin <= :horaFin)
                            )
                        """)
        boolean existeConflictoHorario(
                        @Param("barberoId") Long barberoId,
                        @Param("fecha") LocalDate fecha,
                        @Param("horaInicio") LocalTime horaInicio,
                        @Param("horaFin") LocalTime horaFin);

        // Horas ya ocupadas para un barbero en una fecha
        @Query("""
                            SELECT c.horaInicio, c.estado FROM Cita c
                            WHERE c.barbero.id = :barberoId
                            AND c.fecha = :fecha
                            AND c.estado NOT IN (0)
                        """)
        List<Object[]> findHorasConEstadoPorBarberoYFecha(
                        @Param("barberoId") Long barberoId,
                        @Param("fecha") LocalDate fecha);

        // Para el secretario: citas pendientes de confirmar
        List<Cita> findByEstadoOrderByFechaAscHoraInicioAsc(int estado);

        // Para el secretario: todas las citas de hoy
        List<Cita> findByFechaOrderByHoraInicioAsc(LocalDate fecha);

        List<Cita> findByBarberoAndEstadoAndFechaBetweenOrderByFechaAsc(
                        Barbero barbero,
                        int estado,
                        LocalDate inicio,
                        LocalDate fin);

        @Query("""
                            SELECT c FROM Cita c
                            WHERE c.barbero.id = :barberoId
                            AND c.fecha = :fecha
                            AND c.estado = :estado
                            ORDER BY c.horaInicio ASC
                            LIMIT 1
                        """)
        Optional<Cita> findProximaCitaPorBarberoFechaEstado(
                        @Param("barberoId") Long barberoId,
                        @Param("fecha") LocalDate fecha,
                        @Param("estado") int estado);

        @Query("""
                            SELECT COUNT(c) FROM Cita c
                            WHERE c.cliente.id = :clienteId
                            AND c.estado IN (1, 2)
                        """)
        long contarReservasActivasPorCliente(@Param("clienteId") Long clienteId);

        Page<Cita> findByClienteOrderByFechaDescHoraInicioDesc(Cliente cliente, Pageable pageable);

        // Verificar si cliente tiene cita activa distinta a una específica
        @Query("""
                            SELECT COUNT(c) > 0 FROM Cita c
                            WHERE c.cliente.id = :clienteId
                            AND c.estado IN (1, 2)
                            AND c.id <> :excludeId
                        """)
        boolean tieneOtraReservaActiva(
                        @Param("clienteId") Long clienteId,
                        @Param("excludeId") Long excludeId);

        // ==================== MÉTODO CORREGIDO ====================
        // Reservas confirmadas de hoy con paginación
        Page<Cita> findByFechaAndEstadoOrderByHoraInicioAsc(
                        LocalDate fecha,
                        int estado,
                        Pageable pageable);

       @Query("SELECT c FROM Cita c LEFT JOIN FETCH c.cliente WHERE c.estado = :estado AND c.fecha BETWEEN :inicio AND :fin")
List<Cita> findByEstadoAndFechaBetween(
    @Param("estado") int estado, 
    @Param("inicio") LocalDate inicio, 
    @Param("fin") LocalDate fin
);

    @Query("SELECT COUNT(c) FROM Cita c WHERE c.cliente.id = :clienteId AND c.servicio.id = :servicioId AND c.estado IN (1, 2, 3)")
    long contarCitasPorClienteYServicio(@Param("clienteId") Long clienteId, @Param("servicioId") Long servicioId);
}