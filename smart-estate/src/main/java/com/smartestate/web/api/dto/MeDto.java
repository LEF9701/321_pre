package com.smartestate.web.api.dto;

import com.smartestate.model.User;

/** The signed-in user, as returned to the mobile app. */
public record MeDto(Long id, String name, String email, String role, String language) {

    public static MeDto of(User u) {
        return new MeDto(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getRole() == null ? null : u.getRole().name(),
                u.getLanguage() == null ? null : u.getLanguage().name());
    }
}
