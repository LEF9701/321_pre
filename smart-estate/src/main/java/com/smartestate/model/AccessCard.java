package com.smartestate.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * An Octopus card registered to a resident for estate access. Only the card's printed serial
 * number is held (never any stored value; that data is proprietary and not readable). A card is
 * created / replaced / removed only through an approved {@link CardRegistrationRequest}.
 */
@Entity
public class AccessCard {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String cardNumber;

    @ManyToOne
    private User owner;

    @Enumerated(EnumType.STRING)
    private CardStatus status = CardStatus.ACTIVE;

    private LocalDateTime registeredAt;

    public Long getId() { return id; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public CardStatus getStatus() { return status; }
    public void setStatus(CardStatus status) { this.status = status; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
}
