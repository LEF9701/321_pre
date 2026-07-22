package com.smartestate.model;

public enum SensorType {
    SMOKE("Smoke", "煙霧", true),
    HEAT("Heat", "高溫", true),
    WATER_LEAK("Water leak", "漏水", false),
    // A fire door held open is a fire-SAFETY concern (it must stay shut), but it is not itself a
    // fire, so it raises a warning on the dashboard and does NOT offer the declare-emergency action.
    FIRE_DOOR("Fire door", "防火門", false),
    // A general/service door contact (refuse room, plant room, gate). Informational.
    DOOR("Door", "一般門", false);

    private final String label;
    private final String labelZh;
    /** Fire-family sensors (smoke/heat) can escalate straight to a FIRE emergency. */
    private final boolean fire;

    SensorType(String label, String labelZh, boolean fire) {
        this.label = label;
        this.labelZh = labelZh;
        this.fire = fire;
    }

    public String getLabel() { return label; }
    public String getLabelZh() { return labelZh; }
    public boolean isFire() { return fire; }
    public String label(Language lang) { return lang == Language.ZH ? labelZh : label; }
}
