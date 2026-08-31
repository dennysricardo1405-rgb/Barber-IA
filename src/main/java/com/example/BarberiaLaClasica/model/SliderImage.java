package com.example.BarberiaLaClasica.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "slider_images")
public class SliderImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La URL de la imagen no puede estar vacía")
    @Column(name = "imagen_url", unique = true, nullable = false)
    private String imagenUrl; // Ruta relativa: /images/slider/uuid.png

    @Size(max = 100, message = "El título no puede superar los 100 caracteres")
    @Column(name = "titulo", length = 100)
    private String titulo; // Ej: "Corte Clásico", "Afeitado con Navaja"

    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    @Column(name = "descripcion", length = 255)
    private String descripcion; // Texto promocional visible en el slider

    @Column(name = "orden", nullable = false)
    private Integer orden = 0; // Posición en el carrusel (0, 1, 2...)

    @Column(name = "activo", nullable = false)
    private Boolean activo = true; // Permite ocultar slides sin eliminarlas

    @CreationTimestamp
    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // ── Constructores ──────────────────────────────────────

    public SliderImage() {}

    public SliderImage(String imagenUrl, String titulo, String descripcion, Integer orden) {
        this.imagenUrl   = imagenUrl;
        this.titulo      = titulo;
        this.descripcion = descripcion;
        this.orden       = orden;
        this.activo      = true;
    }

    // ── Getters y Setters ──────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}