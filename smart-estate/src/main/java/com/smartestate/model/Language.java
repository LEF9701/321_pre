package com.smartestate.model;

public enum Language {
    EN("English"),
    ZH("中文");

    private final String label;
    Language(String label) { this.label = label; }
    public String getLabel() { return label; }
}
