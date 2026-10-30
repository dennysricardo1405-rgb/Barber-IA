package com.example.BarberiaLaClasica.dto;

/** Lo que ve el barbero en recepción del corte que el cliente aprobó con el asesor IA. */
public record CorteIAVista(
        String corte,
        String formaRostro,
        Integer mantenimientoSemanas,
        String fotoCliente,
        String fotoCorte) {

    public boolean tieneFotos() {
        return fotoCliente != null || fotoCorte != null;
    }
}
