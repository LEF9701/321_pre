package com.smartestate.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class DeliveryLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Notification notification;

    @ManyToOne(optional = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    private Channel channel;

    private String recipientAddress;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    @Column(length = 1000)
    private String failureReason;
    private boolean escalation;

    /** Provider message id returned by the real gateway (AWS), null for the mock engine. */
    private String providerMessageId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Notification getNotification() { return notification; }
    public void setNotification(Notification notification) { this.notification = notification; }
    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }
    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }
    public String getRecipientAddress() { return recipientAddress; }
    public void setRecipientAddress(String recipientAddress) { this.recipientAddress = recipientAddress; }
    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public boolean isEscalation() { return escalation; }
    public void setEscalation(boolean escalation) { this.escalation = escalation; }
    public String getProviderMessageId() { return providerMessageId; }
    public void setProviderMessageId(String providerMessageId) { this.providerMessageId = providerMessageId; }
}
