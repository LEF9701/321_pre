package com.smartestate.model;

public enum Severity {
    INFO("Info", "一般"),
    WARNING("Warning", "警示"),
    CRITICAL("Critical", "緊急");

    private final String label;
    private final String labelZh;
    Severity(String label, String labelZh) { this.label = label; this.labelZh = labelZh; }
    public String getLabel() { return label; }
    public String getLabelZh() { return labelZh; }
    /** Label in the given language, English for anything but Chinese. */
    public String label(Language lang) { return lang == Language.ZH ? labelZh : label; }
}
