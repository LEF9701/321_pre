package com.smartestate.model;

public enum AccessPointType {
    // Octopus card is used for the shared/common doors only: building entrance and elevator.
    BUILDING_ENTRANCE("Building entrance", "大廈入口"),
    ELEVATOR("Elevator", "升降機");

    private final String label;
    private final String labelZh;

    AccessPointType(String label, String labelZh) { this.label = label; this.labelZh = labelZh; }
    public String getLabel() { return label; }
    public String getLabelZh() { return labelZh; }
    public String label(Language lang) { return lang == Language.ZH ? labelZh : label; }
}
