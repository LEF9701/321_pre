package com.smartestate.messaging.push;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.PushType;
import com.eatthepath.pushy.apns.util.InterruptionLevel;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import com.smartestate.config.ApnsProperties;
import com.smartestate.messaging.ChannelSender;
import com.smartestate.messaging.OutboundMessage;
import com.smartestate.messaging.SendOutcome;
import com.smartestate.model.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * PUSH -> Apple Push Notification service over HTTP/2, using token-based (.p8) auth via the Pushy
 * client. The {@code toAddress} is the device's APNs token. A fire-alarm / critical message
 * ({@link OutboundMessage#isCritical()}) is sent time-sensitive with the loud alarm sound and an
 * {@code alarm} flag the app uses to trigger its full-screen siren; everything else is a normal
 * active-level notification. Follows the same contract as the AWS senders: never throws; provider
 * errors come back as {@link SendOutcome#fail}.
 *
 * <p>The payload is structured so upgrading to Apple's Critical Alerts entitlement
 * later is a one-line change (critical sound + {@code InterruptionLevel.CRITICAL}).
 */
public class PushSender implements ChannelSender {

    private static final Logger log = LoggerFactory.getLogger(PushSender.class);

    private final ApnsClient apnsClient;
    private final ApnsProperties props;

    public PushSender(ApnsClient apnsClient, ApnsProperties props) {
        this.apnsClient = apnsClient;
        this.props = props;
    }

    @Override
    public Channel channel() { return Channel.PUSH; }

    /** Plain path (no notification context); treated as a normal push. */
    @Override
    public SendOutcome send(String toAddress, String subject, String body) {
        return send(OutboundMessage.basic(toAddress, subject, body));
    }

    @Override
    public SendOutcome send(OutboundMessage m) {
        try {
            final boolean critical = m.isCritical();

            final SimpleApnsPayloadBuilder payload = new SimpleApnsPayloadBuilder();
            if (m.subject() != null) payload.setAlertTitle(m.subject());
            payload.setAlertBody(m.body());
            payload.setSound(critical ? props.getAlarmSound() : props.getDefaultSound());
            payload.setInterruptionLevel(critical ? InterruptionLevel.TIME_SENSITIVE : InterruptionLevel.ACTIVE);

            // Custom data the iOS app reads to route/deep-link and to trigger the alarm UI.
            if (critical) {
                payload.addCustomProperty("alarm", Boolean.TRUE);
                payload.setRelevanceScore(1.0);
            }
            if (m.category() != null) payload.addCustomProperty("category", m.category().name());
            if (m.severity() != null) payload.addCustomProperty("severity", m.severity().name());
            if (m.emergencyType() != null) payload.addCustomProperty("emergencyType", m.emergencyType().name());
            if (m.notificationId() != null) payload.addCustomProperty("notificationId", m.notificationId());
            if (m.emergencyEventId() != null) payload.addCustomProperty("emergencyEventId", m.emergencyEventId());

            final String token = TokenUtil.sanitizeTokenString(m.toAddress());
            // A stale fire alarm helps no one: expire criticals fast, keep normal pushes for a day.
            final Instant expiry = Instant.now().plus(critical ? Duration.ofMinutes(10) : Duration.ofHours(24));

            final SimpleApnsPushNotification notification = new SimpleApnsPushNotification(
                    token, props.getBundleId(), payload.build(),
                    expiry, DeliveryPriority.IMMEDIATE, PushType.ALERT);

            final PushNotificationResponse<SimpleApnsPushNotification> response =
                    apnsClient.sendNotification(notification).get(20, TimeUnit.SECONDS);

            if (response.isAccepted()) {
                return SendOutcome.ok(response.getApnsId() != null ? response.getApnsId().toString() : "accepted");
            }
            return SendOutcome.fail(response.getRejectionReason().orElse("rejected by APNs"));
        } catch (Exception e) {
            log.warn("Push send failed to {}: {}", m.toAddress(), e.toString());
            return SendOutcome.fail(e.getMessage());
        }
    }
}
