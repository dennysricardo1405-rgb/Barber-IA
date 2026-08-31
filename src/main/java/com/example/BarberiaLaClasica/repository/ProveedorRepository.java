package com.example.BarberiaLaClasica.repository;

import com.example.BarberiaLaClasica.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    
    // Para listar solo los proveedores activos en el select de compras
    List<Proveedor> findByActivoTrue();
}