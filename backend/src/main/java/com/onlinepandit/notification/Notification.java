package com.onlinepandit.notification;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false, length = 30)
    private String category;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(nullable = false, length = 1000)
    private String message;
    @Column(name = "read_at")
    private Instant readAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    protected Notification() { }
    public Notification(Long userId, String category, String title, String message) {
        this.userId = userId;
        this.category = category;
        this.title = title;
        this.message = message;
    }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void markRead() { if (readAt == null) readAt = Instant.now(); }
}
