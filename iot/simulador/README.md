# Simulador de nodos IoT

Simula un sensor ultrasónico por silla y un DHT22 del local, y publica por MQTT (o HTTP).
Ver la documentación completa en [docs/U3-iot](../../docs/U3-iot/README.md).

| Variable | Por defecto | Descripción |
|---|---|---|
| `MODO` | `mqtt` | `mqtt` o `http` |
| `MQTT_HOST` / `MQTT_PORT` | `localhost` / `1883` | Broker |
| `MQTT_TOPIC_PREFIX` | `laclasica` | Prefijo de los tópicos |
| `HTTP_URL` / `IOT_HTTP_TOKEN` | `http://localhost:8080` / vacío | Para `MODO=http` |
| `SILLAS` | `1,2,3,4` | Ids de las sillas (= id del barbero) |
| `ESCALA_TIEMPO` | `10` | Aceleración del tiempo simulado (1 = tiempo real) |
| `LATIDO_SEG` | `20` | Reenvío del estado aunque no cambie |
| `AMBIENTE_SEG` | `10` | Frecuencia de la lectura de ambiente |
| `UMBRAL_CM` | `70` | Distancia bajo la cual la silla se considera ocupada |
| `SEMILLA` | vacío | Semilla aleatoria para repetir una simulación |
| `DURACION_SEG` | `0` | Termina después de N segundos (0 = nunca) |
