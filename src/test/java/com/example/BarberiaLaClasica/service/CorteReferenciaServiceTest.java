package com.example.BarberiaLaClasica.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.BarberiaLaClasica.model.CorteReferencia;

class CorteReferenciaServiceTest {

    private final List<CorteReferencia> catalogo = List.of(
            corte("Low fade", "/uploads/cortes/low.jpg"),
            corte("Mid fade", "/uploads/cortes/mid.jpg"),
            corte("Pompadour", "/uploads/cortes/pompa.jpg"));

    @Test
    void encuentraElCorteSinImportarMayusculasNiTildes() {
        assertThat(CorteReferenciaService.fotoDe("LOW FADE", catalogo)).isEqualTo("/uploads/cortes/low.jpg");
        assertThat(CorteReferenciaService.fotoDe("Pompadour clásico", catalogo)).isEqualTo("/uploads/cortes/pompa.jpg");
        assertThat(CorteReferenciaService.fotoDe("Low-fade con textura", catalogo)).isEqualTo("/uploads/cortes/low.jpg");
    }

    @Test
    void sinCoincidenciaDevuelveNull() {
        assertThat(CorteReferenciaService.fotoDe("Buzz cut", catalogo)).isNull();
        assertThat(CorteReferenciaService.fotoDe("fa", catalogo)).isNull();
        assertThat(CorteReferenciaService.fotoDe(null, catalogo)).isNull();
    }

    private static CorteReferencia corte(String nombre, String imagen) {
        CorteReferencia c = new CorteReferencia();
        c.setNombre(nombre);
        c.setImagen(imagen);
        return c;
    }
}
