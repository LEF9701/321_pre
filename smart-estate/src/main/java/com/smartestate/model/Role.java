package com.smartestate.model;

public enum Role {
    RESIDENT("Resident", "住戶"),
    MANAGEMENT("Management", "管理處"),
    ADMIN("Admin", "系統管理員");

    private final String label;
    private final String labelZh;
    Role(String label, String labelZh) { this.label = label; this.labelZh = labelZh; }
    public String getLabel() { return label; }
    public String getLabelZh() { return labelZh; }
    /** Label in the given language, English for anything but Chinese. */
    public String label(Language lang) { return lang == Language.ZH ? labelZh : label; }
}
