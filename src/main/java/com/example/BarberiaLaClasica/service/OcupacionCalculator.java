package com.example.BarberiaLaClasica.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.example.BarberiaLaClasica.model.LecturaIot;

/**
 * U3 - Cálculos de ocupación de una silla a partir de sus lecturas del día.
 * Separado del servicio para poder probarlo sin base de datos.
 */
public final class OcupacionCalculator {

    public record Resultado(
            Boolean ocupadaActual,
            LocalDateTime desde,
            LocalDateTime ultimaLectura,
            long segundosOcupada,
            int servicios,
            double minutosPromedioServicio) {

        public double porcentaje(LocalDateTime inicio, LocalDateTime fin) {
            long total = Duration.between(inicio, fin).getSeconds();
            return total <= 0 ? 0 : Math.min(100.0, segundosOcupada * 100.0 / total);
        }
    }

    private OcupacionCalculator() {
    }

    /**
     * @param lecturas lecturas de UNA silla ordenadas por fecha ascendente
     * @param inicio   inicio de la ventana (apertura del local)
     * @param fin      fin de la ventana (ahora)
     */
    public static Resultado calcular(List<LecturaIot> lecturas, LocalDateTime inicio, LocalDateTime fin) {
        Boolean actual = null;
        LocalDateTime desde = null;
        LocalDateTime ultima = null;
        LocalDateTime inicioOcupacion = null;
        long segundos = 0;
        int servicios = 0;
        long segundosServiciosCerrados = 0;
        int serviciosCerrados = 0;

        for (LecturaIot l : lecturas) {
            if (l.getOcupada() == null) {
                continue;
            }
            LocalDateTime t = l.getFecha();
            ultima = t;
            if (actual == null || !actual.equals(l.getOcupada())) {
                if (Boolean.TRUE.equals(l.getOcupada())) {
                    inicioOcupacion = t;
                    servicios++;
                } else if (inicioOcupacion != null) {
                    long dur = segundosEnVentana(inicioOcupacion, t, inicio, fin);
                    segundos += dur;
                    segundosServiciosCerrados += Duration.between(inicioOcupacion, t).getSeconds();
                    serviciosCerrados++;
                    inicioOcupacion = null;
                }
                actual = l.getOcupada();
                desde = t;
            }
        }
        if (Boolean.TRUE.equals(actual) && inicioOcupacion != null) {
            segundos += segundosEnVentana(inicioOcupacion, fin, inicio, fin);
        }
        double promedio = serviciosCerrados == 0 ? 0 : segundosServiciosCerrados / 60.0 / serviciosCerrados;
        return new Resultado(actual, desde, ultima, segundos, servicios, promedio);
    }

    private static long segundosEnVentana(LocalDateTime a, LocalDateTime b, LocalDateTime inicio,
            LocalDateTime fin) {
        LocalDateTime desde = a.isBefore(inicio) ? inicio : a;
        LocalDateTime hasta = b.isAfter(fin) ? fin : b;
        return hasta.isAfter(desde) ? Duration.between(desde, hasta).getSeconds() : 0;
    }
}
