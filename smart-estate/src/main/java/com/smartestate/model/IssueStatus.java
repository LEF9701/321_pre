package com.smartestate.model;

public enum IssueStatus {
    SUBMITTED("Submitted", "已提交"),
    UNDER_REVIEW("Under Review", "審核中"),
    IN_PROGRESS("In Progress", "處理中"),
    RESOLVED("Resolved", "已解決"),
    WITHDRAWN("Withdrawn", "已撤回");

    private final String label;
    private final String labelZh;
    IssueStatus(String label, String labelZh) { this.label = label; this.labelZh = labelZh; }
    public String getLabel() { return label; }
    public String getLabelZh() { return labelZh; }
    /** Label in the given language, English for anything but Chinese. */
    public String label(Language lang) { return lang == Language.ZH ? labelZh : label; }
}
