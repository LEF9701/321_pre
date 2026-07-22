package com.smartestate.model;

import jakarta.persistence.*;

@Entity
@Table(name = "notification_preference",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "category"}))
public class NotificationPreference {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private NoticeCategory category;

    @Enumerated(EnumType.STRING)
    private Channel channel = Channel.EMAIL;

    @Enumerated(EnumType.STRING)
    private Language language = Language.EN;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public NoticeCategory getCategory() { return category; }
    public void setCategory(NoticeCategory category) { this.category = category; }
    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }
    public Language getLanguage() { return language; }
    public void setLanguage(Language language) { this.language = language; }
}
