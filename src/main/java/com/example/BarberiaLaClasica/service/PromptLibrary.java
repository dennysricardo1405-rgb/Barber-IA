package com.example.BarberiaLaClasica.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * U2 - Biblioteca de prompts. Los textos viven en src/main/resources/prompts
 * para poder versionarlos y documentarlos aparte del código.
 */
@Component
public class PromptLibrary {

    public enum Estrategia {
        ZERO_SHOT("asesor-zero-shot"),
        ONE_SHOT("asesor-one-shot"),
        FEW_SHOT("asesor-few-shot");

        private final String archivo;

        Estrategia(String archivo) {
            this.archivo = archivo;
        }

        public String archivo() {
            return archivo;
        }

        public static Estrategia desde(String valor) {
            if (valor == null || valor.isBlank()) {
                return FEW_SHOT;
            }
            return Estrategia.valueOf(valor.trim().toUpperCase().replace('-', '_'));
        }
    }

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String sistemaAsesor() {
        return cargar("asesor-sistema");
    }

    public String usuarioAsesor(Estrategia estrategia, String catalogo) {
        return render(cargar(estrategia.archivo()), Map.of("catalogo", catalogo));
    }

    public String previewImagen(String corte, String descripcion) {
        return render(cargar("preview-imagen"), Map.of(
                "corte", corte == null ? "" : corte,
                "descripcion", descripcion == null ? "" : descripcion));
    }

    String cargar(String nombre) {
        return cache.computeIfAbsent(nombre, n -> {
            try {
                return StreamUtils.copyToString(
                        new ClassPathResource("prompts/" + n + ".txt").getInputStream(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("No se encontró el prompt " + n, e);
            }
        });
    }

    static String render(String plantilla, Map<String, String> variables) {
        String resultado = plantilla;
        for (Map.Entry<String, String> v : variables.entrySet()) {
            resultado = resultado.replace("{{" + v.getKey() + "}}", v.getValue());
        }
        return resultado;
    }
}
