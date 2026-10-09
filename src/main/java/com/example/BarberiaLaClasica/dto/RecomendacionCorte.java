package com.example.BarberiaLaClasica.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/** Estructura JSON que el prompt del asesor exige al modelo (ver prompts/asesor-sistema.txt). */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecomendacionCorte {

    private boolean rostroDetectado;
    private String formaRostro;
    private List<String> rasgos = new ArrayList<>();
    private List<Corte> cortes = new ArrayList<>();
    private String corteRecomendado;
    private String servicioSugerido;
    private String promptImagen;
    private String mensaje;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Corte {
        private String nombre;
        private String motivo;
        private Integer mantenimientoSemanas;
    }

    /** Semanas de mantenimiento del corte recomendado (insumo del recordatorio predictivo de la U4). */
    public Integer mantenimientoDelRecomendado() {
        return cortes.stream()
                .filter(c -> c.getNombre() != null && c.getNombre().equalsIgnoreCase(corteRecomendado))
                .map(Corte::getMantenimientoSemanas)
                .findFirst()
                .orElse(cortes.isEmpty() ? null : cortes.get(0).getMantenimientoSemanas());
    }
}
