package com.example.BarberiaLaClasica.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "productos")
@Data 
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;
    
    private String imagen; 
    
    @Column(nullable = false)
    private double precioVenta;

    private int stock = 0; 

    private boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;
}