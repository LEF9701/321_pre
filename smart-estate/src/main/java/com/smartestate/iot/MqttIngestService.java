package com.smartestate.iot;

import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Subscribes to the estate device fleet over MQTT and routes each message to the matching
 * {@link MqttMessageHandler} (sensors, access control). The broker connection is made only when
 * {@code app.iot.mqtt.enabled=true}; otherwise this is a no-op, so the app still boots with no
 * broker. The connection is fail-soft: if the broker is unreachable the app keeps running and the
 * client auto-reconnects when it comes back.
 */
@Service
public class MqttIngestService {

    private static final Logger log = LoggerFactory.getLogger(MqttIngestService.class);

    private final List<MqttMessageHandler> handlers;
    private final IotEventBus bus;

    @Value("${app.iot.mqtt.enabled:false}")
    private boolean enabled;

    @Value("${app.iot.mqtt.url:tcp://localhost:1883}")
    private String url;

    @Value("${app.iot.mqtt.topic-filter:estate/#}")
    private String topicFilter;

    private MqttClient client;

    public MqttIngestService(List<MqttMessageHandler> handlers, IotEventBus bus) {
        this.handlers = handlers;
        this.bus = bus;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void connect() {
        if (!enabled) {
            log.info("IoT MQTT ingest disabled (set app.iot.mqtt.enabled=true to connect).");
            return;
        }
        try {
            client = new MqttClient(url, "smart-estate-" + UUID.randomUUID().toString().substring(0, 8),
                    new MemoryPersistence());
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setAutomaticReconnect(true);
            opts.setCleanSession(true);
            client.setCallback(new MqttCallbackExtended() {
                @Override public void connectComplete(boolean reconnect, String serverURI) {
                    try {
                        client.subscribe(topicFilter, 1);
                        log.info("IoT MQTT {} {} — subscribed {}", reconnect ? "reconnected" : "connected",
                                serverURI, topicFilter);
                    } catch (MqttException e) {
                        log.warn("IoT MQTT subscribe failed: {}", e.getMessage());
                    }
                }
                @Override public void connectionLost(Throwable cause) {
                    log.warn("IoT MQTT connection lost: {}", cause == null ? "?" : cause.getMessage());
                }
                @Override public void messageArrived(String topic, MqttMessage message) {
                    dispatch(topic, new String(message.getPayload(), StandardCharsets.UTF_8));
                }
                @Override public void deliveryComplete(IMqttDeliveryToken token) { /* publisher only */ }
            });
            client.connect(opts);
        } catch (MqttException e) {
            log.warn("IoT MQTT unavailable at {} ({}); ingest is off until the broker is reachable.",
                    url, e.getMessage());
        }
    }

    private void dispatch(String topic, String payload) {
        try {
            for (MqttMessageHandler h : handlers) {
                if (h.supports(topic)) {
                    h.handle(topic, payload);
                    return;
                }
            }
            // No domain handler yet (e.g. during bring-up): surface it live so the pipe is visible.
            bus.emit("iot.raw", Map.of("topic", topic, "payload", payload));
        } catch (Exception e) {
            log.warn("IoT message handling failed for {}: {}", topic, e.getMessage());
        }
    }

    @PreDestroy
    void disconnect() {
        if (client != null && client.isConnected()) {
            try { client.disconnect(); } catch (MqttException ignored) { }
        }
    }
}
