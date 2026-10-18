package com.onlinepandit.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.onlinepandit.identity.UserAccountRepository;
import com.onlinepandit.marketplace.ActivityRepository;
import com.onlinepandit.marketplace.PanditRepository;
import com.onlinepandit.notification.NotificationService;
import com.onlinepandit.security.JwtAuthenticationFilter.UserPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

class BookingCustomerActionsTest {
    private static final Long BOOKING_ID = 9L;
    private static final Long USER_ID = 4L;
    private static final Long PANDIT_ID = 7L;

    private BookingRepository bookings;
    private PanditRepository pandits;
    private BookingController controller;
    private Booking booking;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        bookings = mock(BookingRepository.class);
        pandits = mock(PanditRepository.class);
        controller = new BookingController(bookings, pandits, mock(ActivityRepository.class),
                mock(NotificationService.class), mock(UserAccountRepository.class), mock(ObjectProvider.class));
        booking = mock(Booking.class);
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(USER_ID, "user@example.com", "USER"));
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(booking.getUserId()).thenReturn(USER_ID);
        when(booking.getId()).thenReturn(BOOKING_ID);
        when(booking.getPanditId()).thenReturn(PANDIT_ID);
        when(booking.getActivityId()).thenReturn(1L);
        when(booking.getScheduledDate()).thenReturn(LocalDate.now().plusDays(1));
        when(booking.getServiceAddress()).thenReturn("Example address");
        when(booking.getTotalAmount()).thenReturn(BigDecimal.valueOf(2500));
        when(booking.getCurrency()).thenReturn("INR");
        when(booking.getPaymentStatus()).thenReturn("PENDING");
        when(booking.getCreatedAt()).thenReturn(Instant.now());
        when(bookings.save(booking)).thenReturn(booking);
        when(pandits.findById(PANDIT_ID)).thenReturn(Optional.empty());
    }

    @Test
    void customerCanCancelPendingBooking() {
        when(booking.getBookingStatus()).thenReturn("PENDING");

        controller.cancel(BOOKING_ID, authentication);

        verify(booking).setBookingStatus("CANCELLED");
        verify(bookings).save(booking);
    }

    @Test
    void customerCannotCancelAnotherUsersBooking() {
        when(booking.getUserId()).thenReturn(88L);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.cancel(BOOKING_ID, authentication));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(bookings, never()).save(booking);
    }

    @Test
    void customerCanRescheduleConfirmedBooking() {
        when(booking.getBookingStatus()).thenReturn("CONFIRMED");
        LocalDate requestedDate = LocalDate.now().plusDays(10);

        controller.reschedule(BOOKING_ID, new BookingController.RescheduleRequest(requestedDate), authentication);

        verify(booking).rescheduleTo(requestedDate);
        verify(bookings).save(booking);
    }

    @Test
    void customerCannotRescheduleCompletedBooking() {
        when(booking.getBookingStatus()).thenReturn("COMPLETED");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.reschedule(BOOKING_ID,
                        new BookingController.RescheduleRequest(LocalDate.now().plusDays(10)), authentication));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(bookings, never()).save(booking);
    }
}