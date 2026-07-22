package com.smartestate.model;

import jakarta.persistence.*;

/** A building bylaw / FAQ document indexed by the RAG chatbot (FR-8.1, FR-8.2). */
@Entity
public class BylawDoc {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    private String title;

    @Enumerated(EnumType.STRING)
    private Language language = Language.EN;

    private String category;

    @Column(length = 8000)
    private String content;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Language getLanguage() { return language; }
    public void setLanguage(Language language) { this.language = language; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
