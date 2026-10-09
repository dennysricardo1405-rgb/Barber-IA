/*
 * U3 - Nodo IoT de la Barbería "La Clásica" (ESP32, simulado en Wokwi).
 *
 * Percepción: 2 sensores ultrasónicos HC-SR04 (uno por silla) + DHT22 (temperatura y humedad).
 * Edge:       el ESP32 decide si la silla está ocupada (distancia < UMBRAL_CM) y enciende un LED.
 * Transmisión: Wi-Fi + MQTT. Publica solo cuando cambia el estado o cada LATIDO_MS.
 *
 * Tópicos:
 *   laclasica/sillas/{id}/estado  {"ocupada":true,"distanciaCm":41.3,"dispositivo":"esp32-sillas"}
 *   laclasica/ambiente            {"temperatura":24.6,"humedad":58.2,"dispositivo":"esp32-sillas"}
 */
#include <WiFi.h>
#include <PubSubClient.h>
#include "DHTesp.h"

// ── Configuración ────────────────────────────────────────────────────────────
const char* WIFI_SSID = "Wokwi-GUEST";
const char* WIFI_PASS = "";
// Wokwi solo llega a brokers públicos. Usa el mismo prefijo en MQTT_TOPIC_PREFIX del sistema.
const char* MQTT_HOST = "broker.hivemq.com";
const int   MQTT_PORT = 1883;
const char* PREFIJO   = "laclasica";
const char* DISPOSITIVO = "esp32-sillas";

const float UMBRAL_CM = 70.0;          // más cerca que esto = hay una persona sentada
const unsigned long LATIDO_MS = 20000; // reenvío periódico aunque no cambie el estado
const unsigned long AMBIENTE_MS = 10000;

struct Silla {
  int id;      // = id del barbero en el sistema ("SILLA #id" en recepción)
  int trig;
  int echo;
  int led;
  int publicado;  // -1 = nunca, 0 = libre, 1 = ocupada
  unsigned long ultimoEnvio;
};

Silla sillas[] = {
  {1, 5, 18, 2, -1, 0},
  {2, 17, 16, 4, -1, 0},
};
const int N_SILLAS = sizeof(sillas) / sizeof(sillas[0]);
const int PIN_DHT = 15;

WiFiClient wifi;
PubSubClient mqtt(wifi);
DHTesp dht;
unsigned long ultimoAmbiente = 0;

float medirDistanciaCm(int trig, int echo) {
  digitalWrite(trig, LOW);
  delayMicroseconds(2);
  digitalWrite(trig, HIGH);
  delayMicroseconds(10);
  digitalWrite(trig, LOW);
  long duracion = pulseIn(echo, HIGH, 30000);  // timeout ~5 m
  if (duracion == 0) return 400.0;
  return duracion * 0.0343 / 2.0;
}

void conectarWifi() {
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  Serial.print("Conectando Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(250);
    Serial.print(".");
  }
  Serial.println(" OK");
}

void conectarMqtt() {
  while (!mqtt.connected()) {
    String id = String("laclasica-esp32-") + String(random(0xffff), HEX);
    Serial.print("Conectando MQTT... ");
    if (mqtt.connect(id.c_str())) {
      Serial.println("OK");
    } else {
      Serial.printf("falló (rc=%d), reintento en 2 s\n", mqtt.state());
      delay(2000);
    }
  }
}

void publicarSilla(Silla& s, bool ocupada, float distancia) {
  char topico[64];
  char payload[128];
  snprintf(topico, sizeof(topico), "%s/sillas/%d/estado", PREFIJO, s.id);
  snprintf(payload, sizeof(payload), "{\"ocupada\":%s,\"distanciaCm\":%.1f,\"dispositivo\":\"%s\"}",
           ocupada ? "true" : "false", distancia, DISPOSITIVO);
  mqtt.publish(topico, payload);
  Serial.printf("-> %s %s\n", topico, payload);
}

void setup() {
  Serial.begin(115200);
  for (int i = 0; i < N_SILLAS; i++) {
    pinMode(sillas[i].trig, OUTPUT);
    pinMode(sillas[i].echo, INPUT);
    pinMode(sillas[i].led, OUTPUT);
  }
  dht.setup(PIN_DHT, DHTesp::DHT22);
  conectarWifi();
  mqtt.setServer(MQTT_HOST, MQTT_PORT);
}

void loop() {
  if (!mqtt.connected()) conectarMqtt();
  mqtt.loop();
  unsigned long ahora = millis();

  for (int i = 0; i < N_SILLAS; i++) {
    Silla& s = sillas[i];
    float distancia = medirDistanciaCm(s.trig, s.echo);
    bool ocupada = distancia < UMBRAL_CM;  // decisión en el borde (edge)
    digitalWrite(s.led, ocupada ? HIGH : LOW);
    if ((int)ocupada != s.publicado || ahora - s.ultimoEnvio >= LATIDO_MS) {
      publicarSilla(s, ocupada, distancia);
      s.publicado = ocupada;
      s.ultimoEnvio = ahora;
    }
  }

  if (ahora - ultimoAmbiente >= AMBIENTE_MS) {
    TempAndHumidity th = dht.getTempAndHumidity();
    if (!isnan(th.temperature)) {
      char topico[64];
      char payload[128];
      snprintf(topico, sizeof(topico), "%s/ambiente", PREFIJO);
      snprintf(payload, sizeof(payload), "{\"temperatura\":%.1f,\"humedad\":%.1f,\"dispositivo\":\"%s\"}",
               th.temperature, th.humidity, DISPOSITIVO);
      mqtt.publish(topico, payload);
      Serial.printf("-> %s %s\n", topico, payload);
    }
    ultimoAmbiente = ahora;
  }
  delay(500);
}
