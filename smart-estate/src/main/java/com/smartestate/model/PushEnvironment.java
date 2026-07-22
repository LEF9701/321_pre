package com.smartestate.model;

/** Which APNs environment a device token was issued for. Sandbox = dev/TestFlight-debug builds. */
public enum PushEnvironment {
    SANDBOX("Sandbox"),
    PRODUCTION("Production");

    private final String label;
    PushEnvironment(String label) { this.label = label; }
    public String getLabel() { return label; }
}
