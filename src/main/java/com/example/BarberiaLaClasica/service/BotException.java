package com.example.BarberiaLaClasica.service;

import java.util.Map;

/** U4: error de negocio que el agente debe explicar al cliente (DNI inválido, horario ocupado, etc.). */
public class BotException extends RuntimeException {

    private final String codigo;
    private final transient Map<String, Object> extra;

    public BotException(String codigo, String mensaje) {
        this(codigo, mensaje, Map.of());
    }

    public BotException(String codigo, String mensaje, Map<String, Object> extra) {
        super(mensaje);
        this.codigo = codigo;
        this.extra = extra;
    }

    public String getCodigo() {
        return codigo;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }
}
