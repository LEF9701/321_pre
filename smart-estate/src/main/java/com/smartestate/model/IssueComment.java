package com.smartestate.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** A comment / update on an issue, posted by the resident (reporter) or by management. */
@Entity
public class IssueComment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Issue issue;

    private String authorName;

    @Enumerated(EnumType.STRING)
    private Role authorRole;

    @Column(length = 2000)
    private String body;

    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Issue getIssue() { return issue; }
    public void setIssue(Issue issue) { this.issue = issue; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public Role getAuthorRole() { return authorRole; }
    public void setAuthorRole(Role authorRole) { this.authorRole = authorRole; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
