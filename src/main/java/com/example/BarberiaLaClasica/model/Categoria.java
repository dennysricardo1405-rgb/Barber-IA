package com.example.BarberiaLaClasica.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "categorias")
@Data // <-- Esto genera automáticamente todos los Getters y Setters
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre; 

    private String descripcion;
    
    private boolean activo = true;

    // Relación Autorreferencial: Muchas subcategorías pertenecen a una categoría Padre
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_padre_id")
    private Categoria padre;

    // Relación inversa: Una categoría padre tiene muchas subcategorías
    @OneToMany(mappedBy = "padre", cascade = CascadeType.ALL)
    private List<Categoria> subcategorias;
}