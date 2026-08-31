package com.example.BarberiaLaClasica.repository;

import com.example.BarberiaLaClasica.model.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromocionRepository extends JpaRepository<Promocion, Long> {

    // Devuelve todas las promociones del sistema ordenadas de la más reciente a la más antigua
    List<Promocion> findAllByOrderByIdDesc();

    // Devuelve las promociones que están marcadas como activas Y que la fecha actual está en su rango de vigencia
    @Query("SELECT p FROM Promocion p WHERE p.activo = true AND :fechaActual BETWEEN p.fechaInicio AND p.fechaFin")
    List<Promocion> findPromocionesVigentes(@Param("fechaActual") LocalDateTime fechaActual);
}