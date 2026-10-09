"""
U2 - Laboratorio de prompts del Asesor de Imagen (Streamlit).

Sube una selfie y compara, lado a lado, cómo responde Gemini con las estrategias
zero-shot, one-shot y few-shot. Usa el mismo endpoint del sistema en Spring Boot
(/api/ia/asesor), así que los prompts que se prueban aquí son los mismos que usa
la página pública /asesor-ia.

Ejecutar:  streamlit run app.py
"""

import os
from pathlib import Path

import requests
import streamlit as st

API = os.getenv("BARBERIA_API_URL", "http://localhost:8080")
PROMPTS = Path(os.getenv("PROMPTS_DIR", Path(__file__).resolve().parents[2] / "src/main/resources/prompts"))
ESTRATEGIAS = {"Zero-shot": "ZERO_SHOT", "One-shot": "ONE_SHOT", "Few-shot": "FEW_SHOT"}

st.set_page_config(page_title="Asesor IA · La Clásica", page_icon="✂️", layout="wide")
st.title("✂️ Asesor de Imagen con IA · Barbería La Clásica")
st.caption(f"Backend: {API} · Modelo: Google Gemini (vía Spring Boot)")

tab_probar, tab_prompts = st.tabs(["Probar estrategias", "Biblioteca de prompts"])

with tab_probar:
    col_foto, col_opciones = st.columns([1, 2])
    with col_foto:
        foto = st.file_uploader("Selfie del cliente", type=["jpg", "jpeg", "png", "webp"])
        if foto:
            st.image(foto, caption="Foto original", use_container_width=True)
    with col_opciones:
        elegidas = st.multiselect("Estrategias a comparar", list(ESTRATEGIAS), default=list(ESTRATEGIAS))
        analizar = st.button("Analizar", type="primary", disabled=not foto or not elegidas)

    if analizar:
        columnas = st.columns(len(elegidas))
        for col, nombre in zip(columnas, elegidas):
            with col:
                st.subheader(nombre)
                with st.spinner("Consultando a Gemini..."):
                    try:
                        resp = requests.post(
                            f"{API}/api/ia/asesor",
                            files={"foto": (foto.name, foto.getvalue(), foto.type)},
                            data={"estrategia": ESTRATEGIAS[nombre]},
                            timeout=120,
                        )
                        datos = resp.json()
                    except requests.RequestException as e:
                        st.error(f"No se pudo conectar con el backend: {e}")
                        continue
                if resp.status_code != 200:
                    st.error(datos.get("error", resp.text))
                    continue
                rec = datos["recomendacion"]
                if datos.get("demo"):
                    st.warning("Modo demostración: el backend no tiene GEMINI_API_KEY")
                if datos.get("previewUrl"):
                    st.image(API + datos["previewUrl"], caption="Vista previa", use_container_width=True)
                st.metric("Corte recomendado", rec.get("corteRecomendado") or "-")
                st.write(f"**Rostro:** {rec.get('formaRostro') or 'no detectado'}")
                st.write(f"**Servicio sugerido:** {rec.get('servicioSugerido') or '-'}")
                st.write(f"**Latencia:** {datos['latenciaMs'] / 1000:.1f} s")
                st.info(rec.get("mensaje") or "")
                with st.expander("JSON completo"):
                    st.json(rec)

with tab_prompts:
    st.write("Archivos versionados en `src/main/resources/prompts/`. Documentación en `docs/U2-ia/biblioteca-prompts.md`.")
    if not PROMPTS.exists():
        st.warning(f"No se encontró la carpeta de prompts en {PROMPTS}")
    else:
        for archivo in sorted(PROMPTS.glob("*.txt")):
            with st.expander(archivo.name):
                st.code(archivo.read_text(encoding="utf-8"), language="markdown")
