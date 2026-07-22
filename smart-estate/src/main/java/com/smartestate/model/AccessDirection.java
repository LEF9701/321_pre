package com.smartestate.model;

public enum AccessDirection {
    IN("Entry", "進入"),
    OUT("Exit", "離開");

    private final String label;
    private final String labelZh;

    AccessDirection(String label, String labelZh) { this.label = label; this.labelZh = labelZh; }
    public String getLabel() { return label; }
    public String getLabelZh() { return labelZh; }
    public String label(Language lang) { return lang == Language.ZH ? labelZh : label; }
}
