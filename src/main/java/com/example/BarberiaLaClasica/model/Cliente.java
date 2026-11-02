package com.example.BarberiaLaClasica.model;
 
import jakarta.persistence.*;
import lombok.Data;

import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
 
@Entity
@Table(name = "clientes")
@Data
public class Cliente {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(nullable = false, unique = true, length = 8)
    private String dni;
 
    @Column(nullable = false)
    private String nombres;
 
    @Column(nullable = false)
    private String apellidos;
 
    private String telefono;
 
    @Column(unique = true)
    private String correo;
 
    private String password;
 
    private int estado = 1;

    // U4: chat de Telegram asociado, para no pedir el DNI en cada conversación
    @Column(name = "telegram_chat_id", length = 60)
    private String telegramChatId;
 
    @CreationTimestamp
    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;

    @Transient
    private int totalVisitas;

    public int getTotalVisitas() {
        return totalVisitas;
    }

    public void setTotalVisitas(int totalVisitas) {
        this.totalVisitas = totalVisitas;
    }
 
    public Cliente() {}
}