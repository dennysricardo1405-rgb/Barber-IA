// ═══════════════════════════════════════════════════════════════════
// ARCHIVO 3: ConfiguracionSitioRepository.java
// ═══════════════════════════════════════════════════════════════════
package com.example.BarberiaLaClasica.repository;
 
import com.example.BarberiaLaClasica.model.ConfiguracionSitio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
 
@Repository
public interface ConfiguracionSitioRepository extends JpaRepository<ConfiguracionSitio, String> {
    List<ConfiguracionSitio> findByGrupo(String grupo);
}