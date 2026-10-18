package com.onlinepandit.feedback;

import com.onlinepandit.booking.Booking;
import com.onlinepandit.booking.BookingRepository;
import com.onlinepandit.marketplace.Pandit;
import com.onlinepandit.marketplace.PanditRepository;
import com.onlinepandit.notification.NotificationService;
import com.onlinepandit.security.JwtAuthenticationFilter.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FeedbackController {
    private final FeedbackRepository feedback;
    private final BookingRepository bookings;
    private final PanditRepository pandits;
    private final NotificationService notifications;
    public FeedbackController(FeedbackRepository feedback, BookingRepository bookings, PanditRepository pandits,
                              NotificationService notifications) {
        this.feedback = feedback;
        this.bookings = bookings;
        this.pandits = pandits;
        this.notifications = notifications;
    }

    @PostMapping("/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public FeedbackResponse submit(@Valid @RequestBody FeedbackRequest request, Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        Booking booking = bookings.findById(request.bookingId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!booking.getUserId().equals(user.id())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (!"COMPLETED".equals(booking.getBookingStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Feedback is available after completion");
        if (feedback.existsByBookingId(booking.getId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Feedback already submitted");
        Feedback saved = feedback.save(new Feedback(booking.getId(), user.id(), booking.getPanditId(), request.rating(), request.comment()));
        Pandit pandit = pandits.findById(booking.getPanditId()).orElseThrow();
        pandit.recordReview(request.rating());
        pandits.save(pandit);
        if (pandit.getUserId() != null) notifications.create(pandit.getUserId(), "FEEDBACK", "New customer feedback",
            "A customer left a " + request.rating() + " star review for your profile.");
        return response(saved);
    }

    @GetMapping("/pandits/{panditId}/feedback")
    public List<FeedbackResponse> publicFeedback(@PathVariable Long panditId) {
        return feedback.findByPanditIdAndModeratedTrueOrderByCreatedAtDesc(panditId).stream().map(FeedbackController::response).toList();
    }

    @GetMapping("/admin/feedback/pending")
    public List<FeedbackResponse> pendingFeedback() {
        return feedback.findByModeratedFalseOrderByCreatedAtAsc().stream().map(FeedbackController::response).toList();
    }

    @PatchMapping("/admin/feedback/{id}/moderation")
    @Transactional
    public FeedbackResponse moderate(@PathVariable Long id, @RequestParam boolean approved) {
        Feedback item = feedback.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        item.setModerated(approved);
        Feedback saved = feedback.save(item);
        notifications.create(saved.getUserId(), "FEEDBACK", approved ? "Feedback published" : "Feedback reviewed",
            approved ? "Your review is now visible on the practitioner profile." : "Your review was reviewed and will remain private.");
        return response(saved);
    }

    private static FeedbackResponse response(Feedback item) {
        return new FeedbackResponse(item.getId(), item.getBookingId(), item.getPanditId(), item.getRating(), item.getComment(), item.isModerated(), item.getCreatedAt());
    }
    public record FeedbackRequest(@Min(1) @Max(5) int rating, String comment, @jakarta.validation.constraints.NotNull Long bookingId) { }
    public record FeedbackResponse(Long id, Long bookingId, Long panditId, int rating, String comment, boolean moderated, java.time.Instant createdAt) { }
}
