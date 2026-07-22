package com.smartestate.model;

public enum IssuePriority {
    LOW("Low", "低"),
    MEDIUM("Medium", "中"),
    HIGH("High", "高");

    private final String label;
    private final String labelZh;
    IssuePriority(String label, String labelZh) { this.label = label; this.labelZh = labelZh; }
    public String getLabel() { return label; }
    public String getLabelZh() { return labelZh; }
    /** Label in the given language, English for anything but Chinese. */
    public String label(Language lang) { return lang == Language.ZH ? labelZh : label; }
}
