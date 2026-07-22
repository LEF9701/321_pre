package com.smartestate.messaging;

import com.smartestate.model.EmergencyType;
import com.smartestate.model.Language;
import com.smartestate.model.NoticeCategory;
import com.smartestate.model.Severity;

/**
 * A richer send request carrying the notification context that some channels need beyond a flat
 * title/body. Push in particular uses {@link #isCritical()} to decide the iOS interruption level
 * and alert sound (a fire alarm is loud + time-sensitive; a general notice is not). Channels that
 * don't care (SMS, Email) read {@link #toAddress()}, {@link #subject()} and {@link #body()}.
 */
public record OutboundMessage(
        String toAddress,
        String subject,
        String body,
        NoticeCategory category,
        Severity severity,
        EmergencyType emergencyType,
        Language language,
        Long notificationId,
        Long emergencyEventId) {

    /** A plain message with no notification context (used by the default sender path). */
    public static OutboundMessage basic(String toAddress, String subject, String body) {
        return new OutboundMessage(toAddress, subject, body, null, null, null, null, null, null);
    }

    /**
     * True when this should be delivered as a forced/critical alert (fire alarm): any emergency
     * event, a CRITICAL severity, or a category that forces all channels (EMERGENCY).
     */
    public boolean isCritical() {
        return emergencyType != null
                || severity == Severity.CRITICAL
                || (category != null && category.isForcedAllChannels());
    }
}
