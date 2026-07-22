package com.smartestate.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Issue {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String refNumber;

    @ManyToOne
    private User reporter;

    @ManyToOne
    private Unit unit;

    @Enumerated(EnumType.STRING)
    private IssueCategory category;

    @Column(length = 4000)
    private String description;

    private String photoFilename;

    @Enumerated(EnumType.STRING)
    private IssuePriority priority = IssuePriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    private IssueStatus status = IssueStatus.SUBMITTED;

    private String assignedTo;

    @Column(length = 2000)
    private String resolutionNote;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRefNumber() { return refNumber; }
    public void setRefNumber(String refNumber) { this.refNumber = refNumber; }
    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public IssueCategory getCategory() { return category; }
    public void setCategory(IssueCategory category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPhotoFilename() { return photoFilename; }
    public void setPhotoFilename(String photoFilename) { this.photoFilename = photoFilename; }
    public IssuePriority getPriority() { return priority; }
    public void setPriority(IssuePriority priority) { this.priority = priority; }
    public IssueStatus getStatus() { return status; }
    public void setStatus(IssueStatus status) { this.status = status; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
