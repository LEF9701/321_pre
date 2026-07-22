package com.smartestate.model;

public enum UnitStatus {
    VACANT("Vacant", "空置"),
    OCCUPIED("Occupied", "已入住");

    private final String label;
    private final String labelZh;
    UnitStatus(String label, String labelZh) { this.label = label; this.labelZh = labelZh; }
    public String getLabel() { return label; }
    public String getLabelZh() { return labelZh; }
    /** Label in the given language, English for anything but Chinese. */
    public String label(Language lang) { return lang == Language.ZH ? labelZh : label; }
}
