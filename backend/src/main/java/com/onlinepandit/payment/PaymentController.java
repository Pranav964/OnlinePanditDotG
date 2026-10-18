package com.onlinepandit.payment;

import com.onlinepandit.security.JwtAuthenticationFilter.UserPrincipal;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService payments;
    private final PaymentRepository paymentRepository;

    public PaymentController(PaymentService payments, PaymentRepository paymentRepository) {
        this.payments = payments;
        this.paymentRepository = paymentRepository;
    }

    @PostMapping("/bookings/{bookingId}/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentService.CheckoutResponse checkout(@PathVariable Long bookingId, Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return payments.createCheckout(bookingId, user.id());
    }

    @PostMapping("/webhooks/stripe")
    @ResponseStatus(HttpStatus.OK)
    public void stripeWebhook(@RequestBody String rawBody, @RequestHeader("Stripe-Signature") @NotBlank String signature) {
        payments.processStripeWebhook(rawBody, signature);
    }

    @GetMapping("/{paymentId}")
    public PaymentDetails payment(@PathVariable Long paymentId, Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        PaymentTransaction payment = payments.getPayment(paymentId, user.id(), user.role());
        return details(payment);
    }

    @GetMapping("/{paymentId}/receipt")
    public ResponseEntity<byte[]> receipt(@PathVariable Long paymentId, Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        PaymentTransaction payment = payments.getPayment(paymentId, user.id(), user.role());
        byte[] pdf = payments.createReceiptPdf(paymentId, user.id(), user.role());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("onlinepandit-receipt-" + payment.getReceiptNumber() + ".pdf", StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @GetMapping("/bookings/{bookingId}/receipt")
    public ResponseEntity<byte[]> bookingReceipt(@PathVariable Long bookingId, Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        byte[] pdf = payments.createReceiptPdfForBooking(bookingId, user.id(), user.role());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("onlinepandit-receipt-booking-" + bookingId + ".pdf", StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @GetMapping("/admin/all")
    public List<PaymentDetails> allPayments(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        if (!"ADMIN".equals(user.role())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return paymentRepository.findAllByOrderByCreatedAtDesc().stream().map(PaymentController::details).toList();
    }

    private static PaymentDetails details(PaymentTransaction payment) {
        return new PaymentDetails(payment.getId(), payment.getBookingId(), payment.getAmount(), payment.getPlatformFee(), payment.getCurrency(),
                payment.getPaymentStatus(), payment.getProvider(), payment.getReceiptNumber(), payment.getPaidAt(), payment.getCreatedAt());
    }

    public record PaymentDetails(Long id, Long bookingId, java.math.BigDecimal amount, java.math.BigDecimal platformFee, String currency,
                                 String paymentStatus, String provider, String receiptNumber,
                                 java.time.Instant paidAt, java.time.Instant createdAt) { }
}
