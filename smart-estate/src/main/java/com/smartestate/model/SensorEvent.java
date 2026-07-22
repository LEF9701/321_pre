package com.smartestate.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** One reading from a {@link Sensor}: the append-only history behind the live dashboard. */
@Entity
public class SensorEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Sensor sensor;

    private String state;                // "ALERT" | "NORMAL" | "OPEN" | "CLOSED" | "LEAK"

    @Enumerated(EnumType.STRING)
    private Severity severity;

    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Sensor getSensor() { return sensor; }
    public void setSensor(Sensor sensor) { this.sensor = sensor; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
