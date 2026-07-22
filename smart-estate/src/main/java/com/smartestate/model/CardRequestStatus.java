package com.smartestate.model;

public enum CardRequestStatus {
    PENDING("Pending approval", "待審批"),
    APPROVED("Approved", "已批准"),
    REJECTED("Rejected", "已拒絕");

    private final String label;
    private final String labelZh;

    CardRequestStatus(String label, String labelZh) { this.label = label; this.labelZh = labelZh; }
    public String getLabel() { return label; }
    public String getLabelZh() { return labelZh; }
    public String label(Language lang) { return lang == Language.ZH ? labelZh : label; }
}
