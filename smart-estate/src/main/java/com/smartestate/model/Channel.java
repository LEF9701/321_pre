package com.smartestate.model;

public enum Channel {
    SMS("SMS", "短訊", true),
    WHATSAPP("WhatsApp", "WhatsApp", true),
    EMAIL("Email", "電郵", true),
    VOICE("Voice Call", "語音來電", false),
    PUSH("Push", "推送通知", true);

    private final String label;
    private final String labelZh;
    private final boolean supportsReadReceipt;
    Channel(String label, String labelZh, boolean supportsReadReceipt) {
        this.label = label;
        this.labelZh = labelZh;
        this.supportsReadReceipt = supportsReadReceipt;
    }
    public String getLabel() { return label; }
    public String getLabelZh() { return labelZh; }
    /** Label in the given language, English for anything but Chinese. */
    public String label(Language lang) { return lang == Language.ZH ? labelZh : label; }
    public boolean isSupportsReadReceipt() { return supportsReadReceipt; }
}
