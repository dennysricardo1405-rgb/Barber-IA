package com.example.BarberiaLaClasica.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "proveedores")
@Data
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    private String telefono;

    @Column(unique = true)
    private String ruc; // Registro Único de Contribuyentes (Perú)

    private boolean activo = true; // Switch de estado activo/inactivo
}