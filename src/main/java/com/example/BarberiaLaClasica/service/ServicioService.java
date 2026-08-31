package com.example.BarberiaLaClasica.service;

import com.example.BarberiaLaClasica.model.Servicio;
import com.example.BarberiaLaClasica.repository.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ServicioService {

    @Autowired
    private ServicioRepository servicioRepository;

    public List<Servicio> listarTodos() {
        return servicioRepository.findAll();
    }

    public List<Servicio> listarActivos() {
        return servicioRepository.findByEstado(1);
    }

    public Servicio buscarPorId(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
    }

    public void guardar(Servicio servicio) {
        if (servicio.getId() == null) {
            servicio.setEstado(1);
        }
        servicioRepository.save(servicio);
    }

    public void cambiarEstado(Long id) {
        Servicio s = buscarPorId(id);
        s.setEstado(s.getEstado() == 1 ? 0 : 1);
        servicioRepository.save(s);
    }

    public long contarActivos() {
        return servicioRepository.findByEstado(1).size();
    }
}