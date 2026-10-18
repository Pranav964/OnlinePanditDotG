package com.onlinepandit.payment;

import com.onlinepandit.identity.UserAccountRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class ReceiptEmailListener {
    private static final Logger logger = LoggerFactory.getLogger(ReceiptEmailListener.class);
    private final JavaMailSender mailSender;
    private final PaymentRepository payments;
    private final UserAccountRepository users;
    private final PaymentService paymentService;
    private final String fromAddress;

    public ReceiptEmailListener(JavaMailSender mailSender, PaymentRepository payments,
                                UserAccountRepository users, PaymentService paymentService,
                                @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.payments = payments;
        this.users = users;
        this.paymentService = paymentService;
        this.fromAddress = fromAddress;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @CircuitBreaker(name = "mailDelivery", fallbackMethod = "mailFallback")
    public void sendReceipt(PaymentConfirmedEvent event) throws Exception {
        PaymentTransaction payment = payments.findById(event.paymentId()).orElseThrow();
        var customer = users.findById(payment.getUserId()).orElseThrow();
        byte[] receipt = paymentService.createReceiptPdf(payment.getId(), customer.getId(), customer.getRole().name());
        var message = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(fromAddress);
        helper.setTo(customer.getEmail());
        helper.setSubject("Your OnlinePanditDotG payment receipt");
        helper.setText("Your payment was verified. The receipt for booking #" + payment.getBookingId() +
                " is attached. Reference: " + payment.getReceiptNumber());
        helper.addAttachment("receipt-" + payment.getReceiptNumber() + ".pdf", new ByteArrayResource(receipt), "application/pdf");
        mailSender.send(message);
    }

    public void mailFallback(PaymentConfirmedEvent event, Throwable error) {
        logger.error("Receipt email delivery failed for payment {}. The receipt remains available in the customer account.", event.paymentId(), error);
    }
}
