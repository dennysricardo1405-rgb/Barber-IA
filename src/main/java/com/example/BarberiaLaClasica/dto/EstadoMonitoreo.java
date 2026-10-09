package com.example.BarberiaLaClasica.dto;

import java.time.LocalDateTime;
import java.util.List;

/** U3 - Foto del estado actual que consume el dashboard IoT. */
public record EstadoMonitoreo(
        LocalDateTime generado,
        List<Silla> sillas,
        Ambiente ambiente,
        List<Punto> serieAmbiente,
        Kpis kpis,
        List<String> alertas) {

    public record Silla(
            Long id,
            String barbero,
            boolean sensorOnline,
            Boolean ocupada,
            LocalDateTime desde,
            long minutosEnEstado,
            boolean sesionEnSistema,
            String alerta,
            double ocupacionHoyPct,
            int serviciosDetectadosHoy) {
    }

    public record Ambiente(Double temperatura, Double humedad, LocalDateTime fecha, boolean online) {
    }

    public record Punto(LocalDateTime fecha, Double temperatura, Double humedad) {
    }

    public record Kpis(
            int sillasOcupadas,
            int sillasTotales,
            double ocupacionPromedioHoyPct,
            int serviciosDetectadosHoy,
            double minutosPromedioServicio,
            int sillasSinRegistro) {
    }
}
