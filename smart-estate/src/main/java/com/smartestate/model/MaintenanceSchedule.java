package com.smartestate.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class MaintenanceSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Building building;

    @ManyToOne
    private Block block;

    private String title;

    @Column(length = 2000)
    private String description;

    private LocalDate scheduledDate;
    private String affectedScope;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Building getBuilding() { return building; }
    public void setBuilding(Building building) { this.building = building; }
    public Block getBlock() { return block; }
    public void setBlock(Block block) { this.block = block; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }
    public String getAffectedScope() { return affectedScope; }
    public void setAffectedScope(String affectedScope) { this.affectedScope = affectedScope; }
}
