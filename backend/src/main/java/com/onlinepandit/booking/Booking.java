package com.onlinepandit.booking;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "pandit_id", nullable = false)
    private Long panditId;
    @Column(name = "activity_id", nullable = false)
    private Long activityId;
    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;
    @Column(name = "service_address", nullable = false, length = 500)
    private String serviceAddress;
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    @Column(nullable = false, length = 3)
    private String currency = "INR";
    @Column(name = "booking_status", nullable = false, length = 24)
    private String bookingStatus = "PENDING";
    @Column(name = "payment_status", nullable = false, length = 24)
    private String paymentStatus = "PENDING";
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Booking() { }
    public Booking(Long userId, Long panditId, Long activityId, LocalDate scheduledDate, String serviceAddress, BigDecimal totalAmount) {
        this.userId = userId;
        this.panditId = panditId;
        this.activityId = activityId;
        this.scheduledDate = scheduledDate;
        this.serviceAddress = serviceAddress;
        this.totalAmount = totalAmount;
    }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getPanditId() { return panditId; }
    public Long getActivityId() { return activityId; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public String getServiceAddress() { return serviceAddress; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public String getBookingStatus() { return bookingStatus; }
    public String getPaymentStatus() { return paymentStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }
    public void rescheduleTo(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }
}
