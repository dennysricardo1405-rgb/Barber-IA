package com.example.BarberiaLaClasica.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "permisos")
@Data
public class Permiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_permiso") 
    private String nombrePermiso; 

    private String nombre;        

    private String descripcion;   

    public Permiso() {}

}