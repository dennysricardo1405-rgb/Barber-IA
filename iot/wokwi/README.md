# Nodo ESP32 en Wokwi

ESP32 con dos HC-SR04 (sillas 1 y 2), un DHT22 y un LED por silla. Publica en `broker.hivemq.com`.

1. Nuevo proyecto ESP32 en https://wokwi.com.
2. Reemplaza `sketch.ino` y `diagram.json`, y crea `libraries.txt` con el contenido de esta carpeta.
3. Ejecuta la simulación y cambia la distancia de los sensores (menos de 70 cm = ocupada).
4. Arranca el sistema con `MQTT_BROKER_URL=tcp://broker.hivemq.com:1883` y el mismo prefijo de tópicos.

Detalles en [docs/U3-iot](../../docs/U3-iot/README.md).
