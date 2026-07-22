package com.smartestate.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * An opaque bearer token issued to a signed-in mobile client. The raw token is returned to the
 * app ONCE at login and never stored; only its SHA-256 hash lives here, so a database leak can't
 * be replayed. Tokens are revocable (logout / lost phone) and expire, unlike a stateless JWT.
 */
@Entity
@Table(name = "api_token",
       uniqueConstraints = @UniqueConstraint(columnNames = "tokenHash"),
       indexes = @Index(name = "idx_api_token_user", columnList = "user_id"))
public class ApiToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    /** Hex SHA-256 of the raw bearer token. */
    @Column(nullable = false, length = 64)
    private String tokenHash;

    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime expiresAt;
    private boolean revoked = false;

    /** Optional human label, e.g. the device model, for the user's session list. */
    private String deviceLabel;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public String getDeviceLabel() { return deviceLabel; }
    public void setDeviceLabel(String deviceLabel) { this.deviceLabel = deviceLabel; }
}
