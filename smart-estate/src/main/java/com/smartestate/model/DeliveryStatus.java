package com.smartestate.model;

public enum DeliveryStatus {
    PENDING("Pending", "待發送"),
    SENT("Sent", "已發送"),
    DELIVERED("Delivered", "已送達"),
    READ("Read", "已閱讀"),
    FAILED("Failed", "發送失敗");

    private final String label;
    private final String labelZh;
    DeliveryStatus(String label, String labelZh) { this.label = label; this.labelZh = labelZh; }
    public String getLabel() { return label; }
    public String getLabelZh() { return labelZh; }
    /** Label in the given language, English for anything but Chinese. */
    public String label(Language lang) { return lang == Language.ZH ? labelZh : label; }
}
