package com.smartestate.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;
    private String actor;
    private String action;

    @Column(length = 2000)
    private String detail;

    // marks records relevant to emergency compliance (FR-7.5)
    private boolean emergencyRelated;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public boolean isEmergencyRelated() { return emergencyRelated; }
    public void setEmergencyRelated(boolean emergencyRelated) { this.emergencyRelated = emergencyRelated; }
}
