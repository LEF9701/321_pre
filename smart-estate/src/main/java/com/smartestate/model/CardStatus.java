package com.smartestate.model;

public enum CardStatus {
    ACTIVE("Active", "生效"),
    SUSPENDED("Suspended", "已停用");

    private final String label;
    private final String labelZh;

    CardStatus(String label, String labelZh) { this.label = label; this.labelZh = labelZh; }
    public String getLabel() { return label; }
    public String getLabelZh() { return labelZh; }
    public String label(Language lang) { return lang == Language.ZH ? labelZh : label; }
}
