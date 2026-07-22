package com.smartestate.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * An APNs (or, later, FCM) device token registered by a resident's phone so the estate can push
 * notifications to it. A single {@link User} may register several devices; each active row is one
 * push destination. Registration/removal happens through the mobile REST API (/api/mobile/devices).
 */
@Entity
@Table(name = "device_token",
       uniqueConstraints = @UniqueConstraint(columnNames = "token"),
       indexes = @Index(name = "idx_device_token_user", columnList = "user_id"))
public class DeviceToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    /** The raw APNs device token (hex). Unique across the system so re-registration re-homes it. */
    @Column(nullable = false, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    private Platform platform = Platform.IOS;

    /** APNs environment this token targets; picks the APNs host when sending. */
    @Enumerated(EnumType.STRING)
    private PushEnvironment environment = PushEnvironment.SANDBOX;

    private String appVersion;

    /** false once the device unregisters (logout/uninstall) or APNs reports the token invalid. */
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime lastSeenAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }
    public PushEnvironment getEnvironment() { return environment; }
    public void setEnvironment(PushEnvironment environment) { this.environment = environment; }
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
