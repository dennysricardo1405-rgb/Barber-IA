package com.example.BarberiaLaClasica.repository;

import com.example.BarberiaLaClasica.model.GastoLocal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GastoLocalRepository extends JpaRepository<GastoLocal, Long> {
    // Para listar los gastos ordenados por fecha recientes
    List<GastoLocal> findAllByOrderByFechaDesc();

    // Para filtrar los gastos por rango de tiempo en las búsquedas
    List<GastoLocal> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);
}