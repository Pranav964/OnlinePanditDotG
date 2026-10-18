package com.onlinepandit.support;

import com.onlinepandit.booking.Booking;
import com.onlinepandit.booking.BookingRepository;
import com.onlinepandit.notification.NotificationService;
import com.onlinepandit.payment.PaymentRepository;
import com.onlinepandit.security.JwtAuthenticationFilter.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComplaintControllerTest {
    @Test
    void customerCannotCreateComplaintForAnotherUsersBooking() {
        ComplaintRepository complaints = mock(ComplaintRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        ComplaintController controller = new ComplaintController(complaints, bookings, payments, mock(NotificationService.class));
        Booking booking = mock(Booking.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(10L, "customer@example.com", "USER"));
        when(bookings.findById(44L)).thenReturn(Optional.of(booking));
        when(booking.getUserId()).thenReturn(99L);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.create(
                new ComplaintController.CreateComplaintRequest(44L, null, "Payment issue", "Please help."), authentication));

        assertEquals(404, exception.getStatusCode().value());
        verify(complaints, never()).save(any(Complaint.class));
    }
}
