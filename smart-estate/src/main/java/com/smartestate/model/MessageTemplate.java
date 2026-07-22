package com.smartestate.model;

import jakarta.persistence.*;

@Entity
public class MessageTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    private String name;

    @Enumerated(EnumType.STRING)
    private NoticeCategory category;

    @Enumerated(EnumType.STRING)
    private Severity severity = Severity.INFO;

    private String titleEn;

    @Column(length = 4000)
    private String contentEn;

    private String titleZh;

    @Column(length = 4000)
    private String contentZh;

    private boolean emergency;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public NoticeCategory getCategory() { return category; }
    public void setCategory(NoticeCategory category) { this.category = category; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public String getTitleEn() { return titleEn; }
    public void setTitleEn(String titleEn) { this.titleEn = titleEn; }
    public String getContentEn() { return contentEn; }
    public void setContentEn(String contentEn) { this.contentEn = contentEn; }
    public String getTitleZh() { return titleZh; }
    public void setTitleZh(String titleZh) { this.titleZh = titleZh; }
    public String getContentZh() { return contentZh; }
    public void setContentZh(String contentZh) { this.contentZh = contentZh; }
    public boolean isEmergency() { return emergency; }
    public void setEmergency(boolean emergency) { this.emergency = emergency; }
}
