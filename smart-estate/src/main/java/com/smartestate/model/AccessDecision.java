package com.smartestate.model;

public enum AccessDecision {
    GRANTED("Granted", "允許"),
    DENIED("Denied", "拒絕");

    private final String label;
    private final String labelZh;

    AccessDecision(String label, String labelZh) { this.label = label; this.labelZh = labelZh; }
    public String getLabel() { return label; }
    public String getLabelZh() { return labelZh; }
    public String label(Language lang) { return lang == Language.ZH ? labelZh : label; }
}
