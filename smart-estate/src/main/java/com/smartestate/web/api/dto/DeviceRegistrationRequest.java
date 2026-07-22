package com.smartestate.web.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Device-token registration body. {@code platform} defaults to IOS and {@code environment} to
 * SANDBOX when omitted (see the controller), so a minimal client only needs to send {@code token}.
 */
public record DeviceRegistrationRequest(
        @NotBlank String token,
        String platform,
        String environment,
        String appVersion) {
}
