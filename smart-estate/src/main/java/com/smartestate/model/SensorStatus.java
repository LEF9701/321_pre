package com.smartestate.model;

public enum SensorStatus {
    NORMAL("Normal", "正常"),
    ALERT("Alert", "警報"),
    OFFLINE("Offline", "離線");

    private final String label;
    private final String labelZh;

    SensorStatus(String label, String labelZh) {
        this.label = label;
        this.labelZh = labelZh;
    }

    public String getLabel() { return label; }
    public String getLabelZh() { return labelZh; }
    public String label(Language lang) { return lang == Language.ZH ? labelZh : label; }
}
