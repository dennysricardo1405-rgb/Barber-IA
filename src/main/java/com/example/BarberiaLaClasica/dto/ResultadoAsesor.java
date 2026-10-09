package com.example.BarberiaLaClasica.dto;

/** Respuesta del endpoint /api/ia/asesor. */
public record ResultadoAsesor(
        Long id,
        String estrategia,
        String modelo,
        boolean demo,
        long latenciaMs,
        String fotoUrl,
        String previewUrl,
        RecomendacionCorte recomendacion) {
}
