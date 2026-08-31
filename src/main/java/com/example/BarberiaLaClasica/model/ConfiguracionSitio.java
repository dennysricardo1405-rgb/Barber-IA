// ═══════════════════════════════════════════════════════════════════
// ARCHIVO 1: ConfiguracionSitio.java  →  model/ConfiguracionSitio.java
// ═══════════════════════════════════════════════════════════════════
package com.example.BarberiaLaClasica.model;
 
import jakarta.persistence.*;
 
@Entity
@Table(name = "configuracion_sitio")
public class ConfiguracionSitio {
 
    @Id
    @Column(name = "clave", length = 100)
    private String clave;          // PK: "logo_url", "telefono", "facebook_url"...
 
    @Column(name = "valor", length = 500)
    private String valor;          // Valor actual del campo
 
    @Column(name = "tipo", length = 30)
    private String tipo;           // "texto" | "imagen" | "url" | "color"
 
    @Column(name = "grupo", length = 50)
    private String grupo;          // "identidad" | "contacto" | "redes" | "horarios"
 
    @Column(name = "etiqueta", length = 100)
    private String etiqueta;       // Nombre legible para el admin
 
    public ConfiguracionSitio() {}
 
    public ConfiguracionSitio(String clave, String valor, String tipo, String grupo, String etiqueta) {
        this.clave    = clave;
        this.valor    = valor;
        this.tipo     = tipo;
        this.grupo    = grupo;
        this.etiqueta = etiqueta;
    }
 
    // Getters / Setters
    public String getClave()    { return clave; }
    public void   setClave(String clave) { this.clave = clave; }
 
    public String getValor()    { return valor; }
    public void   setValor(String valor) { this.valor = valor; }
 
    public String getTipo()     { return tipo; }
    public void   setTipo(String tipo) { this.tipo = tipo; }
 
    public String getGrupo()    { return grupo; }
    public void   setGrupo(String grupo) { this.grupo = grupo; }
 
    public String getEtiqueta() { return etiqueta; }
    public void   setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }
}
 