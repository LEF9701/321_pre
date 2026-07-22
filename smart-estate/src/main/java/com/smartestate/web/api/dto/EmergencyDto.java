package com.smartestate.web.api.dto;

import com.smartestate.model.EmergencyEvent;

import java.time.LocalDateTime;

/** The currently active emergency, for the app's foreground banner / alarm screen. */
public record EmergencyDto(
        Long id,
        String type,
        String label,
        LocalDateTime declaredAt,
        String note) {

    public static EmergencyDto of(EmergencyEvent e) {
        return new EmergencyDto(
                e.getId(),
                e.getType() == null ? null : e.getType().name(),
                e.getType() == null ? null : e.getType().getLabel(),
                e.getDeclaredAt(),
                e.getNote());
    }
}
