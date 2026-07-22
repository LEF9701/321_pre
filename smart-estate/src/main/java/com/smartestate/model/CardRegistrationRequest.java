package com.smartestate.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A resident-initiated change to their access-card registration, held for management/admin approval
 * (maker-checker). Residents may edit their own registration, but the change only takes effect once
 * a committee member or admin approves it.
 */
@Entity
public class CardRegistrationRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User requester;

    private String cardNumber;        // the card being added/replaced (null for REMOVE)

    @Enumerated(EnumType.STRING) private CardRequestAction action;
    @Enumerated(EnumType.STRING) private CardRequestStatus status = CardRequestStatus.PENDING;

    private String decidedByName;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public User getRequester() { return requester; }
    public void setRequester(User requester) { this.requester = requester; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public CardRequestAction getAction() { return action; }
    public void setAction(CardRequestAction action) { this.action = action; }
    public CardRequestStatus getStatus() { return status; }
    public void setStatus(CardRequestStatus status) { this.status = status; }
    public String getDecidedByName() { return decidedByName; }
    public void setDecidedByName(String decidedByName) { this.decidedByName = decidedByName; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
