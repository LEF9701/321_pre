package com.smartestate.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One card tap and its access decision: the behaviour/movement record. This is privacy-sensitive
 * (resident movement), so it is only ever exposed on the management side, never to other residents,
 * and is intended to be retained for a limited window (see AccessService).
 */
@Entity
public class AccessEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cardNumberRaw;     // the number as tapped (kept even when no card matches)

    @ManyToOne private AccessCard card;      // null when the card is unknown
    @ManyToOne private AccessPoint accessPoint;

    @Enumerated(EnumType.STRING) private AccessDirection direction;
    @Enumerated(EnumType.STRING) private AccessDecision decision;

    private String reason;            // why denied, or null when granted
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getCardNumberRaw() { return cardNumberRaw; }
    public void setCardNumberRaw(String cardNumberRaw) { this.cardNumberRaw = cardNumberRaw; }
    public AccessCard getCard() { return card; }
    public void setCard(AccessCard card) { this.card = card; }
    public AccessPoint getAccessPoint() { return accessPoint; }
    public void setAccessPoint(AccessPoint accessPoint) { this.accessPoint = accessPoint; }
    public AccessDirection getDirection() { return direction; }
    public void setDirection(AccessDirection direction) { this.direction = direction; }
    public AccessDecision getDecision() { return decision; }
    public void setDecision(AccessDecision decision) { this.decision = decision; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
