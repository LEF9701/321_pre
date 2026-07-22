package com.smartestate.model;

/** Mobile platform a {@link DeviceToken} belongs to. Only iOS is wired up today. */
public enum Platform {
    IOS("iOS"),
    ANDROID("Android");

    private final String label;
    Platform(String label) { this.label = label; }
    public String getLabel() { return label; }
}
