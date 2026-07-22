package com.smartestate.web.api.dto;

/** Login result: the bearer token to send on every subsequent request, plus the user profile. */
public record LoginResponse(String token, MeDto user) {
}
