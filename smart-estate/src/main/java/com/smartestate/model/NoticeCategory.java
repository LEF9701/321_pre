package com.smartestate.model;

public enum NoticeCategory {
    EMERGENCY("Emergency", "緊急事故", true),
    ANNOUNCEMENT("Announcement", "公告", false),
    MEETING("Meeting / AGM Notice", "會議／業主大會通知", false),
    FEE_REMINDER("Fee Reminder", "費用提醒", false),
    SERVICE_INTERRUPTION("Service Interruption", "服務中斷", false),
    MAINTENANCE("Maintenance", "維修保養", false),
    OBSERVATORY("HK Observatory Alert", "天文台警告", false),
    GENERAL("General", "一般事項", false);

    private final String label;
    private final String labelZh;
    private final boolean forcedAllChannels;
    NoticeCategory(String label, String labelZh, boolean forcedAllChannels) {
        this.label = label;
        this.labelZh = labelZh;
        this.forcedAllChannels = forcedAllChannels;
    }
    public String getLabel() { return label; }
    public String getLabelZh() { return labelZh; }
    /** Label in the given language, English for anything but Chinese. */
    public String label(Language lang) { return lang == Language.ZH ? labelZh : label; }
    public boolean isForcedAllChannels() { return forcedAllChannels; }
}
