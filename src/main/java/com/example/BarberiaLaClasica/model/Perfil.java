package com.example.BarberiaLaClasica.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "perfiles")
@Data
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_perfil") // Según tu imagen
    private String nombrePerfil;

    private String descripcion;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "perfiles_permisos", // Según tu imagen
        joinColumns = @JoinColumn(name = "id_perfil"), // Según tu imagen
        inverseJoinColumns = @JoinColumn(name = "id_permiso") // Según tu imagen
    )
    private List<Permiso> permisos = new ArrayList<>();

    // CONSTRUCTOR VACÍO (Obligatorio)
    public Perfil() {}

}