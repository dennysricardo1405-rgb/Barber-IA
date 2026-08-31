package com.example.BarberiaLaClasica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Entity
@Table(name = "usuarios")
@Data
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Email(message = "Debe ser un correo válido")
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private Integer estado = 1;

    @ManyToOne
    @JoinColumn(name = "id_perfil")
    private Perfil perfil;

    public Usuario() {
    }
    public Usuario(Long id, String nombre, String email, String password, Integer estado, Perfil perfil) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.estado = estado;
        this.perfil = perfil;
    }
}