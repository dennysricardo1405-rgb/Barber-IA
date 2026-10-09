# Biblioteca de prompts del Asesor de Imagen

Todos los prompts viven en `src/main/resources/prompts/` y se cargan con `PromptLibrary`.
Se pueden comparar en vivo con el laboratorio de Streamlit (`ia/streamlit`) o desde `/asesor-ia`.

| Archivo | Tipo | Uso |
|---|---|---|
| `asesor-sistema.txt` | Instrucción de sistema | Rol, tarea, reglas y formato de salida. Se envía en todas las estrategias |
| `asesor-zero-shot.txt` | Zero-shot | Solo catálogo y tarea, sin ejemplos |
| `asesor-one-shot.txt` | One-shot | Un ejemplo (rostro ovalado) |
| `asesor-few-shot.txt` | Few-shot (por defecto) | Cuatro ejemplos: ovalado, redondo, cuadrado y foto sin rostro |
| `preview-imagen.txt` | Instrucción de edición de imagen | Aplica el corte recomendado sin cambiar a la persona |

## Técnicas aplicadas y por qué

| Técnica | Dónde | Justificación |
|---|---|---|
| **Rol (persona)** | `# ROL` del sistema | Un asesor con experiencia local da respuestas en el tono de la barbería y en español peruano |
| **Secciones con encabezados** | `# ROL`, `# TAREA`, `# REGLAS`, `# FORMATO DE SALIDA` | Separa instrucciones de contexto; el modelo las sigue con más consistencia |
| **Delimitadores XML** | `<catalogo>`, `<ejemplos>`, `<ejemplo>`, `<tarea>` | Evita que el modelo confunda el catálogo o los ejemplos con instrucciones |
| **Contexto dinámico** | `{{catalogo}}` se llena con los servicios activos de la base de datos | El servicio sugerido siempre existe en la barbería y se puede reservar |
| **Salida estructurada** | Esquema JSON en el prompt + `responseMimeType: application/json` | El backend la convierte directamente a `RecomendacionCorte`, sin raspar texto |
| **Restricciones explícitas** | Reglas 2, 5 y 7 | Evitan sesgos (no opinar de edad, etnia, piel), nombres de servicio inventados y mensajes largos para WhatsApp |
| **Caso negativo** | Ejemplo 4 del few-shot | Enseña qué hacer si la foto no tiene un rostro, en vez de inventar una forma de rostro |
| **Temperatura baja (0.4)** | `AsesorImagenService` | Recomendaciones consistentes para la misma foto |
| **Prompt en inglés para imagen** | `promptImagen` + `preview-imagen.txt` | Los modelos de imagen siguen mejor descripciones de peinados en inglés; la regla "Keep EXACTLY the same person" preserva la identidad |

## Comparación de estrategias

| Estrategia | Ejemplos | Ventaja | Riesgo esperado |
|---|---|---|---|
| Zero-shot | 0 | Prompt más corto, menor costo y latencia | Nombres de cortes y longitud del `motivo` menos uniformes |
| One-shot | 1 | Fija el estilo y el formato con poco costo | Tiende a copiar el corte del ejemplo (Pompadour) |
| Few-shot | 4 | Cubre formas distintas y el caso sin rostro; la más estable | Prompt más largo, algo más de latencia |

Se eligió **few-shot** como estrategia por defecto. Para la memoria técnica, completen la tabla de
resultados probando las tres estrategias con las mismas fotos en el laboratorio de Streamlit:

| Foto | Zero-shot | One-shot | Few-shot | Latencia (s) |
|---|---|---|---|---|
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |

## Ejemplo de salida

```json
{
  "rostroDetectado": true,
  "formaRostro": "REDONDO",
  "rasgos": ["mejillas llenas", "mandíbula redondeada", "cabello ondulado"],
  "cortes": [
    {"nombre": "Fade alto con volumen arriba", "motivo": "Alarga visualmente el rostro.", "mantenimientoSemanas": 2},
    {"nombre": "Quiff", "motivo": "El volumen frontal crea líneas verticales.", "mantenimientoSemanas": 3},
    {"nombre": "Side part con degradado", "motivo": "La raya lateral rompe la simetría circular.", "mantenimientoSemanas": 3}
  ],
  "corteRecomendado": "Fade alto con volumen arriba",
  "servicioSugerido": "Corte + degradado",
  "promptImagen": "high skin fade, textured voluminous top about 6 cm",
  "mensaje": "Para tu rostro redondo, un Fade alto con volumen arriba estiliza tus facciones. ¡Te esperamos en La Clásica!"
}
```
