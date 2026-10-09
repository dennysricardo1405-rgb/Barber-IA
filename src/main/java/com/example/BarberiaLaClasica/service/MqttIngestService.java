package com.example.BarberiaLaClasica.service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.example.BarberiaLaClasica.dto.LecturaIotRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;

/**
 * U3 - Capa de transmisión: se suscribe al broker MQTT y entrega cada mensaje de los
 * nodos IoT a {@link MonitoreoIotService}.
 *
 * Tópicos (prefijo configurable con MQTT_TOPIC_PREFIX):
 *   {prefijo}/sillas/{idSilla}/estado  -> {"ocupada":true,"distanciaCm":42.5,"dispositivo":"esp32-sillas"}
 *   {prefijo}/ambiente                 -> {"temperatura":24.8,"humedad":61,"dispositivo":"esp32-sillas"}
 */
@Service
@ConditionalOnProperty(name = "iot.mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttIngestService implements MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(MqttIngestService.class);

    private final MonitoreoIotService monitoreo;
    private final ObjectMapper objectMapper;
    private final String brokerUrl;
    private final String prefijo;
    private final String usuario;
    private final String password;
    private MqttClient cliente;

    public MqttIngestService(MonitoreoIotService monitoreo, ObjectMapper objectMapper,
            @Value("${iot.mqtt.broker-url:tcp://localhost:1883}") String brokerUrl,
            @Value("${iot.mqtt.topic-prefix:laclasica}") String prefijo,
            @Value("${iot.mqtt.username:}") String usuario,
            @Value("${iot.mqtt.password:}") String password) {
        this.monitoreo = monitoreo;
        this.objectMapper = objectMapper;
        this.brokerUrl = brokerUrl;
        this.prefijo = prefijo;
        this.usuario = usuario;
        this.password = password;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void conectar() {
        // En un hilo aparte para no bloquear el arranque si el broker aún no está listo
        Thread hilo = new Thread(this::conectarConReintentos, "mqtt-conexion");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void conectarConReintentos() {
        MqttConnectOptions opciones = new MqttConnectOptions();
        opciones.setAutomaticReconnect(true);
        opciones.setCleanSession(true);
        opciones.setConnectionTimeout(10);
        if (!usuario.isBlank()) {
            opciones.setUserName(usuario);
            opciones.setPassword(password.toCharArray());
        }
        int espera = 2;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                cliente = new MqttClient(brokerUrl, "barberia-app-" + UUID.randomUUID().toString().substring(0, 8),
                        new MemoryPersistence());
                cliente.setCallback(this);
                cliente.connect(opciones);
                return; // connectComplete se encarga de suscribirse
            } catch (MqttException e) {
                log.warn("No se pudo conectar al broker MQTT {} ({}). Reintento en {} s", brokerUrl,
                        e.getMessage(), espera);
                try {
                    Thread.sleep(espera * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                espera = Math.min(espera * 2, 60);
            }
        }
    }

    @Override
    public void connectComplete(boolean reconexion, String servidor) {
        try {
            cliente.subscribe(prefijo + "/sillas/+/estado", 1);
            cliente.subscribe(prefijo + "/ambiente", 1);
            log.info("MQTT conectado a {} y suscrito a {}/#", servidor, prefijo);
        } catch (MqttException e) {
            log.error("Error suscribiendo a los tópicos MQTT", e);
        }
    }

    @Override
    public void messageArrived(String topico, MqttMessage mensaje) {
        procesar(topico, new String(mensaje.getPayload(), StandardCharsets.UTF_8));
    }

    /** Público para poder probar el ruteo de tópicos sin un broker real. */
    public void procesar(String topico, String payload) {
        try {
            LecturaIotRequest req = objectMapper.readValue(payload, LecturaIotRequest.class);
            String relativo = topico.startsWith(prefijo + "/") ? topico.substring(prefijo.length() + 1) : topico;
            String[] partes = relativo.split("/");
            if (partes.length == 3 && partes[0].equals("sillas") && partes[2].equals("estado")) {
                monitoreo.registrarSilla(Long.parseLong(partes[1]), req, "MQTT");
            } else if (relativo.equals("ambiente")) {
                monitoreo.registrarAmbiente(req, "MQTT");
            } else {
                log.debug("Tópico ignorado: {}", topico);
            }
        } catch (Exception e) {
            // Un mensaje mal formado no debe tumbar la suscripción
            log.warn("Mensaje MQTT descartado en {}: {}", topico, e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable causa) {
        log.warn("Conexión MQTT perdida: {}. Reconectando...", causa.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Solo nos suscribimos, no publicamos
    }

    public boolean conectado() {
        return cliente != null && cliente.isConnected();
    }

    @PreDestroy
    public void desconectar() {
        try {
            if (cliente != null && cliente.isConnected()) {
                cliente.disconnect();
            }
        } catch (MqttException e) {
            log.debug("Error al desconectar MQTT", e);
        }
    }
}
