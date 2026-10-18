package com.onlinepandit.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.onlinepandit.booking.Booking;
import com.onlinepandit.booking.BookingRepository;
import com.onlinepandit.marketplace.Pandit;
import com.onlinepandit.marketplace.PanditRepository;
import com.onlinepandit.security.JwtAuthenticationFilter.UserPrincipal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FeedbackControllerTest {
    private static final Long BOOKING_ID = 41L;
    private static final Long USER_ID = 10L;
    private static final Long PANDIT_ID = 20L;

    @Mock
    private FeedbackRepository feedback;
    @Mock
    private BookingRepository bookings;
    @Mock
    private PanditRepository pandits;
    @InjectMocks
    private FeedbackController controller;

    @Test
    void submitCompletedBookingSavesFeedbackAndUpdatesPanditRating() {
        Booking booking = mock(Booking.class);
        when(booking.getId()).thenReturn(BOOKING_ID);
        when(booking.getUserId()).thenReturn(USER_ID);
        when(booking.getPanditId()).thenReturn(PANDIT_ID);
        when(booking.getBookingStatus()).thenReturn("COMPLETED");
        Pandit pandit = newPandit();
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(feedback.existsByBookingId(BOOKING_ID)).thenReturn(false);
        when(feedback.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pandits.findById(PANDIT_ID)).thenReturn(Optional.of(pandit));
        when(pandits.save(any(Pandit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FeedbackController.FeedbackResponse response = controller.submit(
                new FeedbackController.FeedbackRequest(4, "Helpful", BOOKING_ID), authenticatedAs(USER_ID));

        assertEquals(4, response.rating());
        assertEquals(4.0, pandit.getRating().doubleValue());
        assertEquals(1, pandit.getReviewCount());
        verify(feedback).save(any(Feedback.class));
        verify(pandits).save(pandit);
    }

    @Test
    void submitRejectsBookingOwnedByAnotherUser() {
        Booking booking = mock(Booking.class);
        when(booking.getUserId()).thenReturn(99L);
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.submit(new FeedbackController.FeedbackRequest(4, null, BOOKING_ID), authenticatedAs(USER_ID)));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(feedback, pandits);
    }

    @Test
    void submitRejectsBookingThatIsNotCompleted() {
        Booking booking = mock(Booking.class);
        when(booking.getUserId()).thenReturn(USER_ID);
        when(booking.getBookingStatus()).thenReturn("PENDING");
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.submit(new FeedbackController.FeedbackRequest(4, null, BOOKING_ID), authenticatedAs(USER_ID)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(feedback, never()).existsByBookingId(BOOKING_ID);
        verifyNoInteractions(pandits);
    }

    @Test
    void submitRejectsDuplicateFeedback() {
        Booking booking = mock(Booking.class);
        when(booking.getId()).thenReturn(BOOKING_ID);
        when(booking.getUserId()).thenReturn(USER_ID);
        when(booking.getBookingStatus()).thenReturn("COMPLETED");
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(feedback.existsByBookingId(BOOKING_ID)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.submit(new FeedbackController.FeedbackRequest(4, null, BOOKING_ID), authenticatedAs(USER_ID)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(feedback, never()).save(any(Feedback.class));
        verifyNoInteractions(pandits);
    }

    private Authentication authenticatedAs(Long userId) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(userId, "test@example.com", "USER"));
        return authentication;
    }

    private static Pandit newPandit() {
        return new Pandit("Test Pandit", 0, null, "City", null, "Country", "Hindu", null, null, null);
    }
}