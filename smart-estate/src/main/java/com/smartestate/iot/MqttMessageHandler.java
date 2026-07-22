package com.smartestate.iot;

/**
 * Handles ingested MQTT messages for a family of topics. Implementations are registered as Spring
 * beans and collected by {@link MqttIngestService}: sensor readings and card taps each get their
 * own handler, so the transport layer stays free of domain logic.
 */
public interface MqttMessageHandler {

    /** True if this handler owns the given topic (e.g. topic.startsWith("estate/sensor/")). */
    boolean supports(String topic);

    /** Process one message. The raw JSON payload is passed through as text. */
    void handle(String topic, String payload);
}
