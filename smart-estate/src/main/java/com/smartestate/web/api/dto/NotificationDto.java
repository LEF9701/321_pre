package com.smartestate.web.api.dto;

import com.smartestate.model.DeliveryLog;
import com.smartestate.model.Language;
import com.smartestate.model.Notification;

import java.time.LocalDateTime;

/** One notification in the resident's mobile inbox, with this device-owner's delivery status. */
public record NotificationDto(
        Long id,
        String title,
        String body,
        String category,
        String severity,
        boolean emergency,
        String status,
        boolean read,
        LocalDateTime sentAt) {

    /** Build from a notification plus the recipient's delivery-log row, picking title and body by language. */
    public static NotificationDto of(Notification n, DeliveryLog log, boolean zh) {
        String title = n.titleFor(zh ? Language.ZH : Language.EN);
        String body = zh ? n.getContentZh() : n.getContentEn();
        if (body == null || body.isBlank()) body = n.getContentEn();
        if (body == null || body.isBlank()) body = title;
        return new NotificationDto(
                n.getId(),
                title,
                body,
                n.getCategory() == null ? null : n.getCategory().name(),
                n.getSeverity() == null ? null : n.getSeverity().name(),
                n.getEmergencyEvent() != null,
                log == null || log.getStatus() == null ? null : log.getStatus().name(),
                log != null && log.getReadAt() != null,
                log == null ? n.getDispatchedAt() : log.getSentAt());
    }
}
