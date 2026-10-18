package com.onlinepandit.payment;

import com.onlinepandit.booking.BookingRepository;
import com.onlinepandit.identity.UserAccountRepository;
import com.onlinepandit.notification.NotificationService;
import com.onlinepandit.marketplace.PanditRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {
    @Test
    void checkoutFailsClosedWhenStripeIsNotConfigured() {
        PaymentRepository payments = mock(PaymentRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        PaymentService service = new PaymentService(payments, bookings, mock(UserAccountRepository.class),
                mock(NotificationService.class), mock(ApplicationEventPublisher.class), mock(PanditRepository.class),
                "", "", "http://localhost:5173/?payment=success", "http://localhost:5173/?payment=cancelled");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createCheckout(1L, 2L));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        verifyNoInteractions(payments, bookings);
    }

    @Test
    void webhookFailsClosedWhenSignatureSecretIsMissing() {
        PaymentService service = new PaymentService(mock(PaymentRepository.class), mock(BookingRepository.class),
                mock(UserAccountRepository.class), mock(NotificationService.class), mock(ApplicationEventPublisher.class),
                mock(PanditRepository.class), "sk_test", "", "http://localhost:5173/?payment=success",
                "http://localhost:5173/?payment=cancelled");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.processStripeWebhook("{}", "signature"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }
}
