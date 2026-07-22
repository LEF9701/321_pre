package com.smartestate.model;

public enum EmergencyType {
    FIRE("Fire", "火警"),
    TYPHOON("Typhoon", "颱風"),
    FLOOD("Flood", "水浸"),
    GAS_LEAK("Gas Leak", "氣體洩漏"),
    STRUCTURAL("Structural", "結構問題"),
    OTHER("Other", "其他");

    private final String label;
    private final String labelZh;
    EmergencyType(String label, String labelZh) { this.label = label; this.labelZh = labelZh; }
    public String getLabel() { return label; }
    public String getLabelZh() { return labelZh; }
    /** Label in the given language, English for anything but Chinese. */
    public String label(Language lang) { return lang == Language.ZH ? labelZh : label; }
}
