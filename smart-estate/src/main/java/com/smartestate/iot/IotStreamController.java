package com.smartestate.iot;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Live dashboard stream. Management/admin browsers open this Server-Sent Events endpoint and receive
 * IoT events (sensor alerts, access taps) as they are ingested. Session-authenticated via the
 * {@code /mgmt/**} rule in SecurityConfig; the browser's EventSource carries the session cookie.
 */
@RestController
@RequestMapping("/mgmt/iot")
public class IotStreamController {

    private final IotEventBus bus;

    public IotStreamController(IotEventBus bus) {
        this.bus = bus;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return bus.subscribe();
    }
}
