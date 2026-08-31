package com.example.BarberiaLaClasica.repository;
 
import com.example.BarberiaLaClasica.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
 
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @Query("SELECT COUNT(c) > 0 FROM Cliente c WHERE c.dni = :dni")
    boolean existsByDni(@Param("dni") String dni);

    @Query("SELECT COUNT(c) > 0 FROM Cliente c WHERE c.correo = :correo")
    boolean existsByCorreo(@Param("correo") String correo);

    @Query("SELECT c FROM Cliente c WHERE c.correo = :correo")
    Optional<Cliente> findByCorreo(@Param("correo") String correo);

    @Query("SELECT c FROM Cliente c WHERE c.dni = :dni")
    Optional<Cliente> findByDni(@Param("dni") String dni);

    @Query("SELECT c FROM Cliente c WHERE c.estado = :estado")
    List<Cliente> findByEstado(@Param("estado") int estado);

    // MODIFICADO: Ahora soporta paginación devolviendo un Page en lugar de un List
    Page<Cliente> findByDniContainingOrNombresContainingIgnoreCaseOrApellidosContainingIgnoreCase(
            String dni, String nombres, String apellidos, Pageable pageable);
}