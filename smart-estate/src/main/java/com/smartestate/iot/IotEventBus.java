package com.smartestate.iot;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Fan-out hub for live dashboard updates. Browsers open a Server-Sent Events stream
 * (see {@link IotStreamController}); ingested IoT events are pushed to every open stream so the
 * management / admin dashboard reacts without a refresh. One-way, server to browser.
 */
@Component
public class IotEventBus {

    private final Set<SseEmitter> emitters = new CopyOnWriteArraySet<>();

    /** Register a new dashboard stream. Never times out; cleaned up on disconnect. */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);   // 0 = no timeout
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> { emitters.remove(emitter); emitter.complete(); });
        emitter.onError(e -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("ready").data("connected"));
        } catch (IOException ignored) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    /** Push a named event with a JSON payload to every connected dashboard. */
    public void emit(String eventName, Object payload) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                emitters.remove(emitter);   // dead stream, drop it
            }
        }
    }

    public int connections() {
        return emitters.size();
    }
}
