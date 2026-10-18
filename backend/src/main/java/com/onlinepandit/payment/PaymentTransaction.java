package com.onlinepandit.payment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class PaymentTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false, length = 24)
    private String provider = "STRIPE";
    @Column(name = "provider_session_id", length = 190)
    private String providerSessionId;
    @Column(name = "provider_payment_id", length = 190)
    private String providerPaymentId;
    @Column(name = "checkout_attempts", nullable = false)
    private int checkoutAttempts;
    @Column(name = "checkout_url", length = 1000)
    private String checkoutUrl;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    @Column(name = "platform_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal platformFee;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(name = "payment_status", nullable = false, length = 24)
    private String paymentStatus = "CREATED";
    @Column(name = "receipt_number", unique = true, length = 40)
    private String receiptNumber;
    @Column(name = "paid_at")
    private Instant paidAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected PaymentTransaction() { }

    public PaymentTransaction(Long bookingId, Long userId, BigDecimal amount, String currency) {
        this(bookingId, userId, amount, currency, BigDecimal.ZERO);
    }

    public PaymentTransaction(Long bookingId, Long userId, BigDecimal amount, String currency, BigDecimal platformFee) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.amount = amount;
        this.platformFee = platformFee;
        this.currency = currency.toUpperCase();
    }

    @PreUpdate
    void updateTimestamp() { updatedAt = Instant.now(); }

    public void attachCheckout(String sessionId, String url) {
        this.providerSessionId = sessionId;
        this.checkoutUrl = url;
    }

    public int startNewCheckoutAttempt() {
        checkoutAttempts++;
        paymentStatus = "CREATED";
        providerSessionId = null;
        checkoutUrl = null;
        providerPaymentId = null;
        return checkoutAttempts;
    }

    public boolean markPaid(String providerPaymentId) {
        if ("PAID".equals(paymentStatus)) return false;
        this.paymentStatus = "PAID";
        this.providerPaymentId = providerPaymentId;
        this.paidAt = Instant.now();
        this.receiptNumber = "OPG-" + paidAt.toString().replaceAll("[-:.TZ]", "").substring(0, 14) + "-" + id;
        return true;
    }

    public void setStatus(String paymentStatus) {
        if (!"PAID".equals(this.paymentStatus)) this.paymentStatus = paymentStatus;
    }

    public Long getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public Long getUserId() { return userId; }
    public String getProvider() { return provider; }
    public String getProviderSessionId() { return providerSessionId; }
    public String getProviderPaymentId() { return providerPaymentId; }
    public int getCheckoutAttempts() { return checkoutAttempts; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getPlatformFee() { return platformFee; }
    public String getCurrency() { return currency; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getReceiptNumber() { return receiptNumber; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getCreatedAt() { return createdAt; }
}
