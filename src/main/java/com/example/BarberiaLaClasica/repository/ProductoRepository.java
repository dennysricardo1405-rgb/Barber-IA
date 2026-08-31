package com.example.BarberiaLaClasica.repository;

import com.example.BarberiaLaClasica.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByStockLessThanEqualAndActivoTrue(int limite);
    List<Producto> findByCategoriaNombreAndActivoTrue(String nombreCategoria);
    List<Producto> findByActivoTrue();
    @Query("SELECT p FROM Producto p " +
       "JOIN FETCH p.categoria c " +
       "LEFT JOIN FETCH c.padre pad " +
       "WHERE p.activo = true AND (c.nombre = :nombreCat OR pad.nombre = :nombreCat)")
List<Producto> findProductosParaCatalogoPublico(@Param("nombreCat") String nombreCat);
long countByStockLessThanEqual(int limite);
}