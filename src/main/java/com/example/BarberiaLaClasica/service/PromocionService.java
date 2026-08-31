package com.example.BarberiaLaClasica.service;

import com.example.BarberiaLaClasica.model.Promocion;
import com.example.BarberiaLaClasica.repository.PromocionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PromocionService {

    private final PromocionRepository repo;

    public PromocionService(PromocionRepository repo) {
        this.repo = repo;
    }

    // Devuelve todas las promociones para el panel administrativo
    public List<Promocion> listarTodas() { 
        return repo.findAllByOrderByIdDesc(); 
    }

    // Devuelve solo las promociones activas y vigentes según la fecha del servidor (Desactivación automática)
    public List<Promocion> listarActivas() { 
        return repo.findPromocionesVigentes(LocalDateTime.now()); 
    }

    // Alternar el estado manual (activar / pausar)
    public void toggleActivo(Long id) {
        Promocion p = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Promoción no encontrada: " + id));
        p.setActivo(!p.isActivo());
        repo.save(p);
    }

    // Eliminar promoción definitivamente
    public void eliminar(Long id) {
        Promocion p = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Promoción no encontrada: " + id));
        repo.delete(p);
    }
    
    // NOTA: El método 'guardar' lo estructuraremos en el siguiente paso cuando creemos el controlador del Admin,
    // ya que ahora recibirá IDs de servicios, productos o categorías en lugar de archivos de imágenes.
}