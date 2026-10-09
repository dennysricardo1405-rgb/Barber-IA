package com.example.BarberiaLaClasica.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/** U3 - Payload JSON que publican los nodos IoT (por MQTT o HTTP). */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LecturaIotRequest {
    // Sensor de silla
    private Boolean ocupada;
    private Double distanciaCm;
    // Sensor de ambiente
    private Double temperatura;
    private Double humedad;
    // Común
    private String dispositivo;
}
