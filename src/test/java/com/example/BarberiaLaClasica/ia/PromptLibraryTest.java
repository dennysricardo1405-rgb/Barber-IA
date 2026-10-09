package com.example.BarberiaLaClasica.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.example.BarberiaLaClasica.service.PromptLibrary;
import com.example.BarberiaLaClasica.service.PromptLibrary.Estrategia;

class PromptLibraryTest {

    private final PromptLibrary prompts = new PromptLibrary();

    @Test
    void sistemaDefineRolReglasYFormatoJson() {
        String sistema = prompts.sistemaAsesor();
        assertThat(sistema).contains("# ROL", "# REGLAS", "# FORMATO DE SALIDA", "\"corteRecomendado\"");
    }

    @Test
    void cadaEstrategiaInsertaElCatalogoEntreDelimitadores() {
        for (Estrategia e : Estrategia.values()) {
            String prompt = prompts.usuarioAsesor(e, "- Corte clásico\n- Barba");
            assertThat(prompt).contains("<catalogo>\n- Corte clásico\n- Barba\n</catalogo>")
                    .doesNotContain("{{");
        }
    }

    @Test
    void zeroOneYFewShotTienen0_1y4Ejemplos() {
        assertThat(contar(prompts.usuarioAsesor(Estrategia.ZERO_SHOT, ""), "Salida:")).isZero();
        assertThat(contar(prompts.usuarioAsesor(Estrategia.ONE_SHOT, ""), "Salida:")).isEqualTo(1);
        assertThat(contar(prompts.usuarioAsesor(Estrategia.FEW_SHOT, ""), "Salida:")).isEqualTo(4);
    }

    @Test
    void estrategiaPorDefectoEsFewShotYAceptaGuiones() {
        assertThat(Estrategia.desde(null)).isEqualTo(Estrategia.FEW_SHOT);
        assertThat(Estrategia.desde("zero-shot")).isEqualTo(Estrategia.ZERO_SHOT);
        assertThatThrownBy(() -> Estrategia.desde("otra")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void promptDeImagenIncluyeElCorte() {
        assertThat(prompts.previewImagen("Crew cut", "short sides"))
                .contains("Crew cut (short sides)")
                .contains("Keep EXACTLY the same person");
    }

    private static int contar(String texto, String patron) {
        return texto.split(java.util.regex.Pattern.quote(patron), -1).length - 1;
    }
}
