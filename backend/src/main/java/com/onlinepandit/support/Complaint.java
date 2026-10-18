package com.onlinepandit.support;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "complaints")
public class Complaint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "booking_id")
    private Long bookingId;
    @Column(name = "payment_id")
    private Long paymentId;
    @Column(nullable = false, length = 160)
    private String subject;
    @Column(nullable = false, length = 2000)
    private String description;
    @Column(nullable = false, length = 24)
    private String status = "OPEN";
    @Column(name = "admin_note", length = 1000)
    private String adminNote;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Complaint() { }
    public Complaint(Long userId, Long bookingId, Long paymentId, String subject, String description) {
        this.userId = userId;
        this.bookingId = bookingId;
        this.paymentId = paymentId;
        this.subject = subject;
        this.description = description;
    }
    @PreUpdate
    void updateTimestamp() { updatedAt = Instant.now(); }
    public void updateStatus(String status, String adminNote) { this.status = status; this.adminNote = adminNote; }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getBookingId() { return bookingId; }
    public Long getPaymentId() { return paymentId; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getAdminNote() { return adminNote; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
