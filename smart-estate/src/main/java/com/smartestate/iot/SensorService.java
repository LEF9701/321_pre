package com.smartestate.iot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartestate.model.*;
import com.smartestate.repo.SensorEventRepository;
import com.smartestate.repo.SensorRepository;
import com.smartestate.service.EmergencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns an incoming sensor reading into a persisted event + live dashboard update, and (for a
 * fire-family sensor going into alert) surfaces the one-click path to a FIRE emergency. The
 * declaration reuses the existing {@link EmergencyService}, so the IoT layer plugs into the
 * emergency + notification pipeline the app already has.
 */
@Service
public class SensorService {

    private static final Logger log = LoggerFactory.getLogger(SensorService.class);

    private final SensorRepository sensorRepo;
    private final SensorEventRepository eventRepo;
    private final EmergencyService emergencyService;
    private final IotEventBus bus;
    private final ObjectMapper json = new ObjectMapper();

    public SensorService(SensorRepository sensorRepo, SensorEventRepository eventRepo,
                         EmergencyService emergencyService, IotEventBus bus) {
        this.sensorRepo = sensorRepo;
        this.eventRepo = eventRepo;
        this.emergencyService = emergencyService;
        this.bus = bus;
    }

    /** Ingest a reading published by the device fleet. Unknown sensor codes are ignored (logged). */
    @Transactional
    public void ingest(String sensorCode, String payload) {
        Sensor sensor = sensorRepo.findByCode(sensorCode).orElse(null);
        if (sensor == null) {
            log.debug("Reading for unknown sensor {} ignored.", sensorCode);
            return;
        }
        String state;
        Severity severity;
        try {
            JsonNode n = json.readTree(payload);
            state = n.hasNonNull("state") ? n.get("state").asText().toUpperCase() : "ALERT";
            severity = n.hasNonNull("severity")
                    ? Severity.valueOf(n.get("severity").asText().toUpperCase())
                    : (isAlarming(state) ? Severity.WARNING : Severity.INFO);
        } catch (Exception e) {
            log.warn("Bad sensor payload for {}: {}", sensorCode, e.getMessage());
            return;
        }

        SensorEvent event = new SensorEvent();
        event.setSensor(sensor);
        event.setState(state);
        event.setSeverity(severity);
        event.setCreatedAt(LocalDateTime.now());
        eventRepo.save(event);

        sensor.setStatus(isAlarming(state) ? SensorStatus.ALERT : SensorStatus.NORMAL);
        sensor.setLastSeverity(severity);
        sensor.setLastEventAt(event.getCreatedAt());
        sensorRepo.save(sensor);

        bus.emit("sensor", view(sensor, state));
    }

    /**
     * FR-7 bridge: declare a FIRE emergency straight from a sensor, with its location as the note,
     * then let the existing emergency flow (broadcast, iOS alarm) take over.
     */
    @Transactional
    public String declareFromSensor(Long sensorId) {
        Sensor sensor = sensorRepo.findById(sensorId).orElseThrow();
        String location = sensor.locationLabel();
        emergencyService.declare(EmergencyType.FIRE,
                "Auto-flagged by sensor " + sensor.getCode() + " — " + location);
        return location;
    }

    /** Every sensor as a dashboard view, for the initial page load (SSE keeps it fresh after). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> allViews() {
        return sensorRepo.findAllByOrderByCodeAsc().stream().map(s -> view(s, null)).toList();
    }

    /** A dashboard-shaped view of a sensor (enum NAMEs; the React side localises the labels). */
    public Map<String, Object> view(Sensor s, String state) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("code", s.getCode());
        m.put("type", s.getType() == null ? null : s.getType().name());
        m.put("status", s.getStatus() == null ? null : s.getStatus().name());
        m.put("severity", s.getLastSeverity() == null ? null : s.getLastSeverity().name());
        m.put("location", s.locationLabel());
        m.put("fireType", s.isFireType());
        m.put("state", state);
        m.put("at", s.getLastEventAt() == null ? null : s.getLastEventAt().toString());
        return m;
    }

    private boolean isAlarming(String state) {
        return switch (state) {
            case "NORMAL", "CLEAR", "CLOSED", "OK" -> false;
            default -> true;   // ALERT, OPEN, LEAK, TRIGGERED, etc.
        };
    }
}
