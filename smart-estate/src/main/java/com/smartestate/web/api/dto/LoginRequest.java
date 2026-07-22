package com.smartestate.web.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Mobile login body. */
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password) {
}
