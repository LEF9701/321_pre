package com.smartestate.iot;

import org.springframework.stereotype.Component;

/** Routes {@code estate/access/{accessPointCode}} card taps to {@link AccessService}. */
@Component
public class AccessMqttHandler implements MqttMessageHandler {

    private static final String PREFIX = "estate/access/";

    private final AccessService accessService;

    public AccessMqttHandler(AccessService accessService) {
        this.accessService = accessService;
    }

    @Override
    public boolean supports(String topic) {
        return topic != null && topic.startsWith(PREFIX);
    }

    @Override
    public void handle(String topic, String payload) {
        String code = topic.substring(PREFIX.length());
        if (!code.isBlank()) accessService.ingest(code, payload);
    }
}
