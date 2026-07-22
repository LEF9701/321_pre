package com.smartestate.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class EmergencyEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private EmergencyType type;

    private String declaredBy;
    private LocalDateTime declaredAt;
    private LocalDateTime closedAt;
    private boolean active;

    @Column(length = 1000)
    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EmergencyType getType() { return type; }
    public void setType(EmergencyType type) { this.type = type; }
    public String getDeclaredBy() { return declaredBy; }
    public void setDeclaredBy(String declaredBy) { this.declaredBy = declaredBy; }
    public LocalDateTime getDeclaredAt() { return declaredAt; }
    public void setDeclaredAt(LocalDateTime declaredAt) { this.declaredAt = declaredAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
