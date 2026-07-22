package com.smartestate.service;

import com.smartestate.config.ChannelPolicy;
import com.smartestate.messaging.MessageDispatcher;
import com.smartestate.messaging.OutboundMessage;
import com.smartestate.messaging.SendOutcome;
import com.smartestate.model.*;
import com.smartestate.repo.DeliveryLogRepository;
import com.smartestate.repo.DeviceTokenRepository;
import com.smartestate.repo.NotificationPreferenceRepository;
import com.smartestate.repo.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Multi-channel delivery engine (FR-1). Channel routing is delegated to {@link MessageDispatcher}:
 * when AWS messaging is enabled each channel is sent through its AWS service
 * (SMS/Voice -> End User Messaging, Email -> SES, WhatsApp -> End User Messaging Social); when it is
 * disabled (default) the engine falls back to a deterministic mock that records a DeliveryLog row
 * with a simulated status, so the app runs and demos fully offline. Either way every send produces a
 * DeliveryLog capturing channel, recipient address, provider id and status for the analytics views.
 */
@Service
public class NotificationDispatchService {

    private final NotificationRepository notificationRepository;
    private final DeliveryLogRepository deliveryLogRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final MessageDispatcher dispatcher;
    private final ChannelPolicy channelPolicy;

    /** For a real AWS send, whether "accepted by AWS" is recorded as DELIVERED (true) or as
     *  SENT/unconfirmed (false). Default true so the analytics reflect a successful hand-off.
     *  Set to false once real delivery-receipt callbacks (SNS event destinations) advance the
     *  status from SENT -> DELIVERED/FAILED. */
    @org.springframework.beans.factory.annotation.Value("${app.aws.messaging.assume-delivered:true}")
    private boolean assumeDelivered;

    public NotificationDispatchService(NotificationRepository notificationRepository,
                                       DeliveryLogRepository deliveryLogRepository,
                                       NotificationPreferenceRepository preferenceRepository,
                                       DeviceTokenRepository deviceTokenRepository,
                                       MessageDispatcher dispatcher,
                                       ChannelPolicy channelPolicy) {
        this.notificationRepository = notificationRepository;
        this.deliveryLogRepository = deliveryLogRepository;
        this.preferenceRepository = preferenceRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.dispatcher = dispatcher;
        this.channelPolicy = channelPolicy;
    }

    /**
     * Dispatch a notification to recipients.
     * Normal mode (FR-1.2): one channel per resident, taken from their per-category preference.
     * Emergency mode (FR-7.2): force SMS + WhatsApp + Email for everyone, ignoring preferences;
     * Voice is held back for the escalation step (FR-7.3).
     */
    public int dispatch(Notification n, List<User> recipients, boolean forceAllChannels) {
        return dispatch(n, recipients, forceAllChannels, false);
    }

    /**
     * @param simulateOnly when true, always use the mock engine even if AWS is enabled.
     *        Used by the DataSeeder so seeded demo history never triggers real (billable) sends.
     */
    @Transactional
    public int dispatch(Notification n, List<User> recipients, boolean forceAllChannels, boolean simulateOnly) {
        LocalDateTime now = LocalDateTime.now();
        n.setDispatched(true);
        n.setDispatchedAt(now);
        n.setForcedAllChannels(forceAllChannels);
        notificationRepository.save(n);

        int count = 0;
        for (User u : recipients) {
            List<Channel> channels;
            if (forceAllChannels) {
                // Forced/emergency: every enabled digital channel incl. push (Voice is held for escalation).
                channels = new ArrayList<>();
                for (Channel c : List.of(Channel.SMS, Channel.WHATSAPP, Channel.EMAIL, Channel.PUSH)) {
                    if (channelPolicy.isEnabled(c)) channels.add(c);
                }
            } else {
                Channel pref = preferenceRepository.findByUserAndCategory(u, n.getCategory())
                        .map(NotificationPreference::getChannel)
                        .orElse(channelPolicy.defaultChannel());
                if (!channelPolicy.isEnabled(pref)) pref = channelPolicy.defaultChannel(); // coerce disabled prefs
                List<Channel> resolved = new ArrayList<>();
                resolved.add(pref);
                // App users (a registered device) also get a push for every notice, so the mobile app
                // is a first-class channel regardless of the SMS/email preference. Residents without a
                // registered device are skipped inside createPushLogs, so this is a no-op for them.
                if (channelPolicy.isEnabled(Channel.PUSH) && pref != Channel.PUSH) {
                    resolved.add(Channel.PUSH);
                }
                channels = resolved;
            }
            for (Channel ch : channels) {
                if (ch == Channel.PUSH) {
                    createPushLogs(n, u, false, now, simulateOnly);
                } else {
                    createLog(n, u, ch, false, now, simulateOnly);
                }
            }
            count++;
        }
        return count;
    }

    /**
     * Voice-call escalation (FR-7.3): auto-call every recipient whose other channels are all
     * still undelivered. In production this fires 2 minutes after broadcast; in the prototype the
     * admin triggers it on demand so the "2-minute timer" outcome can be demonstrated immediately.
     */
    public int runVoiceEscalation(Notification n) {
        return runVoiceEscalation(n, false);
    }

    @Transactional
    public int runVoiceEscalation(Notification n, boolean simulateOnly) {
        if (!channelPolicy.isEnabled(Channel.VOICE)) return 0; // voice disabled -> nothing to escalate to
        List<DeliveryLog> logs = deliveryLogRepository.findByNotificationOrderByIdAsc(n);
        Map<Long, List<DeliveryLog>> byUser = logs.stream()
                .filter(l -> l.getRecipient() != null)
                .collect(Collectors.groupingBy(l -> l.getRecipient().getId()));

        LocalDateTime now = LocalDateTime.now();
        int escalated = 0;
        for (List<DeliveryLog> userLogs : byUser.values()) {
            boolean alreadyVoiced = userLogs.stream().anyMatch(l -> l.getChannel() == Channel.VOICE);
            if (alreadyVoiced) continue;
            boolean reached = userLogs.stream()
                    .anyMatch(l -> l.getStatus() == DeliveryStatus.DELIVERED || l.getStatus() == DeliveryStatus.READ);
            if (reached) continue; // another channel already got through
            User u = userLogs.get(0).getRecipient();
            createLog(n, u, Channel.VOICE, true, now, simulateOnly);
            escalated++;
        }
        return escalated;
    }

    /** Read receipt (FR-1.3): called when a resident opens the notification in their inbox. */
    @Transactional
    public void markRead(Notification n, User u) {
        if (n == null || u == null) return;
        List<DeliveryLog> logs = deliveryLogRepository.findByNotification_IdAndRecipient_Id(n.getId(), u.getId());
        LocalDateTime now = LocalDateTime.now();
        for (DeliveryLog l : logs) {
            if (l.getChannel().isSupportsReadReceipt() && l.getStatus() == DeliveryStatus.DELIVERED) {
                l.setStatus(DeliveryStatus.READ);
                l.setReadAt(now);
            }
        }
    }

    public List<DeliveryLog> logsFor(Notification n) {
        return deliveryLogRepository.findByNotificationOrderByIdAsc(n);
    }

    public DeliveryStats statsFor(Notification n) {
        return DeliveryStats.of(logsFor(n));
    }

    // ---- internal helpers -------------------------------------------------

    private void createLog(Notification n, User u, Channel ch, boolean escalation, LocalDateTime now,
                           boolean simulateOnly) {
        DeliveryLog log = new DeliveryLog();
        log.setNotification(n);
        log.setRecipient(u);
        log.setChannel(ch);
        String address = addressFor(u, ch);
        log.setRecipientAddress(address);
        log.setSentAt(now);
        log.setEscalation(escalation);

        if (!simulateOnly && dispatcher.supports(ch)) {
            // Real send through the AWS service for this channel.
            SendOutcome out = dispatcher.send(ch, address, titleFor(n, u), bodyFor(n, u));
            if (out.accepted()) {
                // AWS accepted the message. Without delivery-receipt callbacks we can't confirm
                // handset delivery, so treat "accepted" as DELIVERED (assumeDelivered) or leave it
                // SENT/unconfirmed for later advancement by real callbacks.
                if (assumeDelivered) {
                    log.setStatus(DeliveryStatus.DELIVERED);
                    log.setDeliveredAt(now);
                } else {
                    log.setStatus(DeliveryStatus.SENT);
                }
                log.setProviderMessageId(truncate(out.providerId(), 255));
            } else {
                log.setStatus(DeliveryStatus.FAILED);
                log.setFailureReason(truncate(out.error(), 1000));
            }
        } else {
            // Mock engine (offline default): deterministic simulated outcome.
            DeliveryStatus status = simulate(ch, n.getId(), u.getId());
            log.setStatus(status);
            if (status == DeliveryStatus.DELIVERED) {
                log.setDeliveredAt(now);
            } else if (status == DeliveryStatus.FAILED) {
                log.setFailureReason(failureReason(ch));
            }
        }
        deliveryLogRepository.save(log);
    }

    /**
     * Push is one-to-many: a resident may have several devices. Fan out to every active device
     * token, one DeliveryLog per token, carrying the notification context so the sender can render a
     * fire-alarm (loud + time-sensitive) vs a normal push. A resident with no registered device
     * gets nothing on this channel; their other channels still cover them.
     */
    private void createPushLogs(Notification n, User u, boolean escalation, LocalDateTime now,
                                boolean simulateOnly) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserAndActiveTrue(u);
        for (DeviceToken dt : tokens) {
            DeliveryLog log = new DeliveryLog();
            log.setNotification(n);
            log.setRecipient(u);
            log.setChannel(Channel.PUSH);
            log.setRecipientAddress(dt.getToken());
            log.setSentAt(now);
            log.setEscalation(escalation);

            if (!simulateOnly && dispatcher.supports(Channel.PUSH)) {
                SendOutcome out = dispatcher.send(Channel.PUSH, pushMessage(n, u, dt.getToken()));
                if (out.accepted()) {
                    if (assumeDelivered) {
                        log.setStatus(DeliveryStatus.DELIVERED);
                        log.setDeliveredAt(now);
                    } else {
                        log.setStatus(DeliveryStatus.SENT);
                    }
                    log.setProviderMessageId(truncate(out.providerId(), 255));
                } else {
                    log.setStatus(DeliveryStatus.FAILED);
                    log.setFailureReason(truncate(out.error(), 1000));
                }
            } else {
                DeliveryStatus status = simulate(Channel.PUSH, n.getId(), u.getId());
                log.setStatus(status);
                if (status == DeliveryStatus.DELIVERED) {
                    log.setDeliveredAt(now);
                } else if (status == DeliveryStatus.FAILED) {
                    log.setFailureReason(failureReason(Channel.PUSH));
                }
            }
            deliveryLogRepository.save(log);
        }
    }

    /** Assemble the push payload context (title/body + category/severity/emergency) for one device. */
    private OutboundMessage pushMessage(Notification n, User u, String deviceToken) {
        EmergencyType emergencyType = n.getEmergencyEvent() != null ? n.getEmergencyEvent().getType() : null;
        Long emergencyEventId = n.getEmergencyEvent() != null ? n.getEmergencyEvent().getId() : null;
        Language lang = u != null ? u.getLanguage() : Language.EN;
        return new OutboundMessage(deviceToken, titleFor(n, u), bodyFor(n, u),
                n.getCategory(), n.getSeverity(), emergencyType, lang, n.getId(), emergencyEventId);
    }

    /** Pick the title in the recipient's language (FR-1.2), falling back to the English one. */
    private String titleFor(Notification n, User u) {
        return n.titleFor(u != null ? u.getLanguage() : Language.EN);
    }

    /** Pick the content in the recipient's language (FR-1.2), falling back to English then the title. */
    private String bodyFor(Notification n, User u) {
        boolean zh = u != null && u.getLanguage() == Language.ZH;
        String body = zh ? n.getContentZh() : n.getContentEn();
        if (body == null || body.isBlank()) body = n.getContentEn();
        if (body == null || body.isBlank()) body = titleFor(n, u);
        return body;
    }

    private String addressFor(User u, Channel ch) {
        return switch (ch) {
            case EMAIL -> u.getEmail() == null ? "(no email)" : u.getEmail();
            case SMS, WHATSAPP, VOICE -> u.getPhone() == null || u.getPhone().isBlank() ? "(no phone)" : u.getPhone();
            // Push resolves to per-device tokens in createPushLogs, not through this single-address path.
            case PUSH -> "(push)";
        };
    }

    /**
     * Deterministic simulated outcome so analytics are stable across page reloads.
     * ~82% delivered, ~10% sent-but-unconfirmed, ~8% failed for digital channels;
     * voice calls have a higher answer rate.
     */
    private DeliveryStatus simulate(Channel ch, Long notifId, Long userId) {
        int h = Math.floorMod(Objects.hash(notifId, userId, ch.name()), 100);
        if (ch == Channel.VOICE) {
            return h < 90 ? DeliveryStatus.DELIVERED : DeliveryStatus.FAILED;
        }
        if (h < 82) return DeliveryStatus.DELIVERED;
        if (h < 92) return DeliveryStatus.SENT;
        return DeliveryStatus.FAILED;
    }

    private String failureReason(Channel ch) {
        return switch (ch) {
            case SMS -> "Carrier rejected / number unreachable";
            case WHATSAPP -> "No WhatsApp account on number";
            case EMAIL -> "Mailbox bounced";
            case VOICE -> "No answer after 3 attempts";
            case PUSH -> "Device unreachable / token expired";
        };
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
