package com.smartestate.messaging;

/** Result of handing a message to an external provider: accepted (with a provider id) or failed. */
public record SendOutcome(boolean accepted, String providerId, String error) {
    public static SendOutcome ok(String providerId) { return new SendOutcome(true, providerId, null); }
    public static SendOutcome fail(String error) { return new SendOutcome(false, null, error); }
}
