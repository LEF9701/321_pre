package com.smartestate.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** English title. Paired with {@link #titleZh}; see titleFor() in NotificationDispatchService. */
    private String title;

    /** Traditional Chinese title. Optional; falls back to {@link #title} when management leaves it blank. */
    private String titleZh;

    @Column(length = 4000)
    private String contentEn;

    @Column(length = 4000)
    private String contentZh;

    @Enumerated(EnumType.STRING)
    private NoticeCategory category;

    @Enumerated(EnumType.STRING)
    private Severity severity = Severity.INFO;

    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime dispatchedAt;
    private boolean dispatched;
    private boolean forcedAllChannels;

    private String recipientScope;

    // structured recipient filter so the scheduler can re-resolve recipients later
    private String scopeType;   // ALL | BUILDING | BLOCK
    private Long scopeId;

    @ManyToOne
    private EmergencyEvent emergencyEvent;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTitleZh() { return titleZh; }
    public void setTitleZh(String titleZh) { this.titleZh = titleZh; }

    /** The title in the reader's language, falling back to the English one. */
    public String titleFor(Language lang) {
        if (lang == Language.ZH && titleZh != null && !titleZh.isBlank()) return titleZh;
        return title;
    }
    public String getContentEn() { return contentEn; }
    public void setContentEn(String contentEn) { this.contentEn = contentEn; }
    public String getContentZh() { return contentZh; }
    public void setContentZh(String contentZh) { this.contentZh = contentZh; }
    public NoticeCategory getCategory() { return category; }
    public void setCategory(NoticeCategory category) { this.category = category; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public LocalDateTime getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(LocalDateTime dispatchedAt) { this.dispatchedAt = dispatchedAt; }
    public boolean isDispatched() { return dispatched; }
    public void setDispatched(boolean dispatched) { this.dispatched = dispatched; }
    public boolean isForcedAllChannels() { return forcedAllChannels; }
    public void setForcedAllChannels(boolean forcedAllChannels) { this.forcedAllChannels = forcedAllChannels; }
    public String getRecipientScope() { return recipientScope; }
    public void setRecipientScope(String recipientScope) { this.recipientScope = recipientScope; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public Long getScopeId() { return scopeId; }
    public void setScopeId(Long scopeId) { this.scopeId = scopeId; }
    public EmergencyEvent getEmergencyEvent() { return emergencyEvent; }
    public void setEmergencyEvent(EmergencyEvent emergencyEvent) { this.emergencyEvent = emergencyEvent; }
}
