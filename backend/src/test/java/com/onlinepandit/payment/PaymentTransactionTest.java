package com.onlinepandit.payment;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PaymentTransactionTest {
    @Test
    void checkoutAttemptResetsExpiredSessionState() {
        PaymentTransaction payment = new PaymentTransaction(12L, 4L, new BigDecimal("6500.00"), "INR");
        assertEquals(1, payment.startNewCheckoutAttempt());
        payment.attachCheckout("cs_expired", "https://checkout.example/expired");
        payment.setStatus("EXPIRED");

        assertEquals(2, payment.startNewCheckoutAttempt());
        assertEquals("CREATED", payment.getPaymentStatus());
        assertNull(payment.getProviderSessionId());
        assertNull(payment.getCheckoutUrl());
    }

    @Test
    void paidStateAndReceiptAreCreatedOnlyOnce() {
        PaymentTransaction payment = new PaymentTransaction(12L, 4L, new BigDecimal("6500.00"), "INR");
        payment.startNewCheckoutAttempt();
        payment.attachCheckout("cs_123", "https://checkout.example/123");

        assertTrue(payment.markPaid("pi_123"));
        String receipt = payment.getReceiptNumber();
        assertNotNull(receipt);
        assertEquals("PAID", payment.getPaymentStatus());
        assertFalse(payment.markPaid("pi_duplicate"));
        assertEquals(receipt, payment.getReceiptNumber());
        assertEquals("pi_123", payment.getProviderPaymentId());
    }
}
