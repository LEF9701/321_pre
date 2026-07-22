package com.smartestate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Config-driven Apple Push Notification service settings (mirrors {@link AwsMessagingProperties}).
 * NOTHING is hard-coded: the .p8 token-auth key, team/key/bundle ids and environment all come from
 * {@code app.push.apns.*}. When {@code enabled=false} (default) no APNs client/sender bean exists,
 * so {@link com.smartestate.messaging.MessageDispatcher#supports} is false for PUSH and the
 * dispatcher falls back to the mock engine (the app still runs fully offline).
 */
@Component
@ConfigurationProperties(prefix = "app.push.apns")
public class ApnsProperties {

    /** Master switch. false -> mock/simulated push (offline). */
    private boolean enabled = false;

    /** SANDBOX (dev/debug builds) or PRODUCTION (App Store / TestFlight release builds). */
    private String environment = "SANDBOX";

    /** Apple Developer Team ID (10 chars). */
    private String teamId;

    /** APNs Auth Key ID (the .p8 key's Key ID). */
    private String keyId;

    /** App bundle id, used as the APNs topic, e.g. hk.smartestate.app. */
    private String bundleId;

    /** Filesystem path to the AuthKey_XXXX.p8 token-signing key. */
    private String p8KeyPath;

    /** Sound file (in the app bundle) for normal notifications. */
    private String defaultSound = "default";

    /** Loud sound file (in the app bundle) for fire-alarm / critical pushes. */
    private String alarmSound = "fire_alarm.caf";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }
    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }
    public String getBundleId() { return bundleId; }
    public void setBundleId(String bundleId) { this.bundleId = bundleId; }
    public String getP8KeyPath() { return p8KeyPath; }
    public void setP8KeyPath(String p8KeyPath) { this.p8KeyPath = p8KeyPath; }
    public String getDefaultSound() { return defaultSound; }
    public void setDefaultSound(String defaultSound) { this.defaultSound = defaultSound; }
    public String getAlarmSound() { return alarmSound; }
    public void setAlarmSound(String alarmSound) { this.alarmSound = alarmSound; }
}
