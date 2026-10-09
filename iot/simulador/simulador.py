"""
U3 - Simulador de nodos IoT de la Barbería "La Clásica".

Simula un sensor ultrasónico (HC-SR04) por silla y un sensor DHT22 de temperatura y
humedad del local. Igual que el ESP32 real, la decisión "ocupada / libre" se toma en el
borde (edge): si la distancia medida es menor que UMBRAL_CM hay una persona sentada.

Publica en MQTT (por defecto) o por HTTP:
  {prefijo}/sillas/{id}/estado  {"ocupada": true, "distanciaCm": 41.3, "dispositivo": "sim-silla-1"}
  {prefijo}/ambiente            {"temperatura": 24.6, "humedad": 58.2, "dispositivo": "sim-ambiente"}

Variables de entorno: ver README.md de esta carpeta.
"""

import json
import math
import os
import random
import signal
import sys
import time
import urllib.request

MODO = os.getenv("MODO", "mqtt").lower()            # mqtt | http
BROKER_HOST = os.getenv("MQTT_HOST", "localhost")
BROKER_PORT = int(os.getenv("MQTT_PORT", "1883"))
PREFIJO = os.getenv("MQTT_TOPIC_PREFIX", "laclasica")
HTTP_URL = os.getenv("HTTP_URL", "http://localhost:8080")
HTTP_TOKEN = os.getenv("IOT_HTTP_TOKEN", "")
SILLAS = [int(s) for s in os.getenv("SILLAS", "1,2,3,4").split(",") if s.strip()]
ESCALA = float(os.getenv("ESCALA_TIEMPO", "10"))     # 10 = un corte de 30 min dura 3 min reales
LATIDO_S = float(os.getenv("LATIDO_SEG", "20"))      # reenvía el estado aunque no cambie
AMBIENTE_S = float(os.getenv("AMBIENTE_SEG", "10"))
UMBRAL_CM = float(os.getenv("UMBRAL_CM", "70"))
SEMILLA = os.getenv("SEMILLA")
DURACION_S = float(os.getenv("DURACION_SEG", "0"))   # 0 = sin límite (útil en CI)

if SEMILLA:
    random.seed(int(SEMILLA))


class Silla:
    """Máquina de estados de una silla: libre -> ocupada (servicio) -> libre."""

    def __init__(self, sid):
        self.id = sid
        self.ocupada = False
        self.fin = 0.0
        self.ultimo_envio = 0.0
        self.publicado = None

    def actualizar(self, ahora):
        if self.ocupada and ahora >= self.fin:
            self.ocupada = False
        elif not self.ocupada:
            # Probabilidad de que llegue un cliente: ~1 cada 25 minutos simulados por silla
            prob_por_seg = ESCALA / (25 * 60)
            if random.random() < prob_por_seg:
                self.ocupada = True
                minutos = random.uniform(20, 45)  # duración típica de un corte
                self.fin = ahora + minutos * 60 / ESCALA

    def distancia(self):
        # Lo que "mide" el HC-SR04: cerca si hay alguien sentado, lejos (pared) si no
        return round(random.uniform(30, 55) if self.ocupada else random.uniform(120, 220), 1)


class Ambiente:
    def __init__(self):
        self.t0 = time.time()

    def leer(self, ocupadas):
        horas = (time.time() - self.t0) * ESCALA / 3600
        temp = 23 + 2.5 * math.sin(horas / 24 * 2 * math.pi) + 0.6 * ocupadas + random.gauss(0, 0.2)
        hum = 60 - 4 * math.sin(horas / 24 * 2 * math.pi) + 1.5 * ocupadas + random.gauss(0, 0.8)
        return round(temp, 1), round(max(20, min(95, hum)), 1)


class PublicadorMqtt:
    def __init__(self):
        import paho.mqtt.client as mqtt

        self.cliente = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=f"simulador-{random.randint(0, 9999)}")
        usuario = os.getenv("MQTT_USER")
        if usuario:
            self.cliente.username_pw_set(usuario, os.getenv("MQTT_PASS", ""))
        while True:
            try:
                self.cliente.connect(BROKER_HOST, BROKER_PORT, keepalive=30)
                break
            except OSError as e:
                print(f"[simulador] broker {BROKER_HOST}:{BROKER_PORT} no disponible ({e}); reintento en 3 s", flush=True)
                time.sleep(3)
        self.cliente.loop_start()

    def silla(self, sid, datos):
        self.cliente.publish(f"{PREFIJO}/sillas/{sid}/estado", json.dumps(datos), qos=1)

    def ambiente(self, datos):
        self.cliente.publish(f"{PREFIJO}/ambiente", json.dumps(datos), qos=1)


class PublicadorHttp:
    def _post(self, ruta, datos):
        req = urllib.request.Request(
            HTTP_URL + ruta, data=json.dumps(datos).encode(), method="POST",
            headers={"Content-Type": "application/json", "X-IOT-TOKEN": HTTP_TOKEN})
        try:
            urllib.request.urlopen(req, timeout=5).read()
        except Exception as e:  # el servidor puede estar reiniciando
            print(f"[simulador] error HTTP {ruta}: {e}", flush=True)

    def silla(self, sid, datos):
        self._post(f"/api/iot/sillas/{sid}", datos)

    def ambiente(self, datos):
        self._post("/api/iot/ambiente", datos)


def main():
    publicador = PublicadorHttp() if MODO == "http" else PublicadorMqtt()
    sillas = [Silla(s) for s in SILLAS]
    ambiente = Ambiente()
    ultimo_ambiente = 0.0
    inicio = time.time()
    print(f"[simulador] modo={MODO} sillas={SILLAS} escala={ESCALA}x prefijo={PREFIJO}", flush=True)

    while True:
        ahora = time.time()
        for s in sillas:
            s.actualizar(ahora)
            distancia = s.distancia()
            ocupada = distancia < UMBRAL_CM  # decisión en el borde
            if ocupada != s.publicado or ahora - s.ultimo_envio >= LATIDO_S:
                publicador.silla(s.id, {"ocupada": ocupada, "distanciaCm": distancia, "dispositivo": f"sim-silla-{s.id}"})
                if ocupada != s.publicado:
                    print(f"[simulador] silla {s.id} -> {'OCUPADA' if ocupada else 'libre'} ({distancia} cm)", flush=True)
                s.publicado = ocupada
                s.ultimo_envio = ahora

        if ahora - ultimo_ambiente >= AMBIENTE_S:
            temp, hum = ambiente.leer(sum(1 for s in sillas if s.ocupada))
            publicador.ambiente({"temperatura": temp, "humedad": hum, "dispositivo": "sim-ambiente"})
            ultimo_ambiente = ahora

        if DURACION_S and ahora - inicio >= DURACION_S:
            print("[simulador] fin de la simulación", flush=True)
            return
        time.sleep(1)


if __name__ == "__main__":
    signal.signal(signal.SIGTERM, lambda *_: sys.exit(0))
    main()
