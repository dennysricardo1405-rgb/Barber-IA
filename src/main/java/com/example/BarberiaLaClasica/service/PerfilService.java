package com.example.BarberiaLaClasica.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.BarberiaLaClasica.model.Perfil;
import com.example.BarberiaLaClasica.model.Permiso;
import com.example.BarberiaLaClasica.repository.PerfilRepository;
import com.example.BarberiaLaClasica.repository.PermisoRepository;

import jakarta.transaction.Transactional;

@Service
public class PerfilService {
    @Autowired
    private PerfilRepository perfilRepository;
    @Autowired
    private PermisoRepository permisoRepository;

    public List<Perfil> listarTodo() { return perfilRepository.findAll(); }
    public List<Permiso> listarPermisos() { return permisoRepository.findAll(); }

    @Transactional
    public void guardarPerfilConPermisos(Long perfilId, List<Long> permisoIds) {
        Perfil perfil = perfilRepository.findById(perfilId).orElseThrow();
        List<Permiso> permisos = permisoRepository.findAllById(permisoIds);
        perfil.setPermisos(permisos);
        perfilRepository.save(perfil);
    }
}
