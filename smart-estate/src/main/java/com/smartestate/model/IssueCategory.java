package com.smartestate.model;

public enum IssueCategory {
    PLUMBING("Plumbing", "水管渠務"),
    ELECTRICAL("Electrical", "電力"),
    AC_HEATING("Air-con / Heating", "冷氣／暖氣"),
    SECURITY("Security", "保安"),
    COMMON_AREA("Common Area", "公共地方"),
    NOISE("Noise", "噪音"),
    OTHER("Other", "其他");

    private final String label;
    private final String labelZh;
    IssueCategory(String label, String labelZh) { this.label = label; this.labelZh = labelZh; }
    public String getLabel() { return label; }
    public String getLabelZh() { return labelZh; }
    /** Label in the given language, English for anything but Chinese. */
    public String label(Language lang) { return lang == Language.ZH ? labelZh : label; }
}
