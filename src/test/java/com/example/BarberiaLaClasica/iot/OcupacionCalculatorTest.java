package com.example.BarberiaLaClasica.iot;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.BarberiaLaClasica.model.LecturaIot;
import com.example.BarberiaLaClasica.service.OcupacionCalculator;

class OcupacionCalculatorTest {

    private static final LocalDateTime APERTURA = LocalDateTime.of(2026, 10, 9, 9, 0);

    private static LecturaIot lectura(int minuto, boolean ocupada) {
        LecturaIot l = new LecturaIot();
        l.setTipo(LecturaIot.SILLA);
        l.setSillaId(1L);
        l.setOcupada(ocupada);
        l.setFecha(APERTURA.plusMinutes(minuto));
        return l;
    }

    @Test
    void sumaElTiempoOcupadoYCuentaServicios() {
        // Ocupada 9:10-9:40 (30 min) y 10:00-10:20 (20 min); latidos repetidos no cuentan como servicio nuevo
        List<LecturaIot> lecturas = List.of(
                lectura(0, false), lectura(10, true), lectura(20, true), lectura(40, false),
                lectura(60, true), lectura(80, false));
        OcupacionCalculator.Resultado r = OcupacionCalculator.calcular(lecturas, APERTURA, APERTURA.plusMinutes(100));

        assertThat(r.servicios()).isEqualTo(2);
        assertThat(r.segundosOcupada()).isEqualTo(50 * 60);
        assertThat(r.minutosPromedioServicio()).isEqualTo(25.0);
        assertThat(r.porcentaje(APERTURA, APERTURA.plusMinutes(100))).isEqualTo(50.0);
        assertThat(r.ocupadaActual()).isFalse();
        assertThat(r.desde()).isEqualTo(APERTURA.plusMinutes(80));
    }

    @Test
    void unServicioEnCursoCuentaHastaAhora() {
        OcupacionCalculator.Resultado r = OcupacionCalculator.calcular(
                List.of(lectura(30, true)), APERTURA, APERTURA.plusMinutes(60));
        assertThat(r.ocupadaActual()).isTrue();
        assertThat(r.segundosOcupada()).isEqualTo(30 * 60);
        assertThat(r.minutosPromedioServicio()).isZero();
    }

    @Test
    void ignoraElTiempoAntesDeLaApertura() {
        OcupacionCalculator.Resultado r = OcupacionCalculator.calcular(
                List.of(lectura(-30, true), lectura(15, false)), APERTURA, APERTURA.plusMinutes(60));
        assertThat(r.segundosOcupada()).isEqualTo(15 * 60);
    }

    @Test
    void sinLecturasNoHayEstado() {
        OcupacionCalculator.Resultado r = OcupacionCalculator.calcular(List.of(), APERTURA, APERTURA.plusHours(1));
        assertThat(r.ocupadaActual()).isNull();
        assertThat(r.porcentaje(APERTURA, APERTURA.plusHours(1))).isZero();
    }
}
