package com.smartestate.model;

public enum FeeStatus {
    PAID("Paid", "已繳費"),
    DUE("Due", "待繳費"),
    OVERDUE("Overdue", "逾期未繳");

    private final String label;
    private final String labelZh;
    FeeStatus(String label, String labelZh) { this.label = label; this.labelZh = labelZh; }
    public String getLabel() { return label; }
    public String getLabelZh() { return labelZh; }
    /** Label in the given language, English for anything but Chinese. */
    public String label(Language lang) { return lang == Language.ZH ? labelZh : label; }
}
