package com.smartestate.model;

public enum CardRequestAction {
    ADD("Register a new card", "登記新卡"),
    REPLACE("Replace my card", "更換卡片"),
    REMOVE("Remove my card", "移除卡片");

    private final String label;
    private final String labelZh;

    CardRequestAction(String label, String labelZh) { this.label = label; this.labelZh = labelZh; }
    public String getLabel() { return label; }
    public String getLabelZh() { return labelZh; }
    public String label(Language lang) { return lang == Language.ZH ? labelZh : label; }
}
