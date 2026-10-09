# Barbería "La Clásica": sistema inteligente y automatizado

Proyecto final del curso **Herramientas de Desarrollo Profesional TIC** (2026-2).
Sistema web en Spring Boot para la Barbería "La Clásica" (Chiclayo) con reservas, recepción de sillas,
ventas, inventario y reportes, más los componentes de tecnologías emergentes que pide el curso.

| Unidad | Componente | Dónde está | Documentación |
|---|---|---|---|
| U1 | DevOps y CI/CD: Docker, docker compose, pipeline con pruebas y despliegue simulado | `Dockerfile`, `docker-compose.yml`, `.github/workflows/ci-cd.yml`, `src/test` | [docs/U1-devops](docs/U1-devops/README.md) |
| U2 | IA y prompt engineering: asesor de imagen con Google Gemini | `src/main/resources/prompts`, `AsesorImagenService`, `/asesor-ia`, `ia/streamlit` | [docs/U2-ia](docs/U2-ia/README.md) |
| U3 | IoT: sensores de ocupación de sillas y ambiente, MQTT y dashboard en tiempo real | `iot/`, `MqttIngestService`, `MonitoreoIotService`, `/monitoreo` | [docs/U3-iot](docs/U3-iot/README.md) |
| U4 | RPA / No-Code (n8n + WhatsApp) | pendiente (semana 13) | |
| U5 | AR / VR | pendiente (semana 16) | |

## Ejecutar con Docker (recomendado)

```bash
cp .env.example .env          # completa las claves (GEMINI_API_KEY es opcional)
docker compose --profile iot up -d --build
```

| Servicio | URL |
|---|---|
| Sistema web | http://localhost:8080 (admin: `admin@gmail.com` / `ADMIN_PASSWORD`) |
| Asesor de imagen IA | http://localhost:8080/asesor-ia |
| Dashboard IoT | http://localhost:8080/monitoreo (administrador o secretario) |
| Broker MQTT | `localhost:1883` |
| Laboratorio de prompts (Streamlit) | `docker compose --profile ia up -d` y luego http://localhost:8501 |

## Ejecutar sin Docker (como en la laptop)

1. MySQL local y un archivo `.env` con `DB_URL`, `DB_USER`, `DB_PASS`, `MAIL_*`, `API_DNI_TOKEN`.
2. (Opcional) un broker MQTT en `localhost:1883`, por ejemplo `docker run -p 1883:1883 eclipse-mosquitto:2.0 mosquitto -c /mosquitto-no-auth.conf`.
3. `./mvnw spring-boot:run`
4. (Opcional) el simulador IoT: `pip install -r iot/simulador/requirements.txt && python iot/simulador/simulador.py`

Si no hay broker, la aplicación arranca igual y reintenta la conexión MQTT en segundo plano.

## Pruebas

```bash
./mvnw verify      # 29 pruebas con H2 en memoria + reporte de cobertura en target/site/jacoco
```
