package com.smartestate.iot;

import org.springframework.stereotype.Component;

/**
 * Routes {@code estate/sensor/{sensorCode}} messages to {@link SensorService}. The sensor is
 * identified by the last topic segment; the payload carries state + severity.
 */
@Component
public class SensorMqttHandler implements MqttMessageHandler {

    private static final String PREFIX = "estate/sensor/";

    private final SensorService sensorService;

    public SensorMqttHandler(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @Override
    public boolean supports(String topic) {
        return topic != null && topic.startsWith(PREFIX);
    }

    @Override
    public void handle(String topic, String payload) {
        String code = topic.substring(PREFIX.length());
        if (!code.isBlank()) sensorService.ingest(code, payload);
    }
}
