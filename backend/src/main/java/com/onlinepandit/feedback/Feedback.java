package com.onlinepandit.feedback;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "feedback")
public class Feedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "pandit_id", nullable = false)
    private Long panditId;
    @Column(nullable = false)
    private int rating;
    @Column(length = 1000)
    private String comment;
    @Column(nullable = false)
    private boolean moderated;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    protected Feedback() { }
    public Feedback(Long bookingId, Long userId, Long panditId, int rating, String comment) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.panditId = panditId;
        this.rating = rating;
        this.comment = comment;
    }
    public Long getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public Long getUserId() { return userId; }
    public Long getPanditId() { return panditId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public boolean isModerated() { return moderated; }
    public Instant getCreatedAt() { return createdAt; }
    public void setModerated(boolean moderated) { this.moderated = moderated; }
}
