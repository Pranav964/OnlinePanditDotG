package com.onlinepandit.booking;

import com.onlinepandit.identity.UserAccountRepository;
import com.onlinepandit.marketplace.Activity;
import com.onlinepandit.marketplace.ActivityRepository;
import com.onlinepandit.marketplace.Pandit;
import com.onlinepandit.marketplace.PanditRepository;
import com.onlinepandit.notification.NotificationService;
import com.onlinepandit.security.JwtAuthenticationFilter.UserPrincipal;
import com.onlinepandit.notification.SmsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingRepository bookings;
    private final PanditRepository pandits;
    private final ActivityRepository activities;
    private final NotificationService notifications;
    private final UserAccountRepository users;
    private final ObjectProvider<BookingEventPublisher> eventPublisher;
    private final ObjectProvider<SmsService> smsService;

    public BookingController(BookingRepository bookings, PanditRepository pandits, ActivityRepository activities,
                             NotificationService notifications, UserAccountRepository users,
                             ObjectProvider<BookingEventPublisher> eventPublisher,
                             ObjectProvider<SmsService> smsService) {
        this.bookings = bookings;
        this.pandits = pandits;
        this.activities = activities;
        this.notifications = notifications;
        this.users = users;
        this.eventPublisher = eventPublisher;
        this.smsService = smsService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request, Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        if (request.scheduledDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking date must be today or later");
        }
        Pandit pandit = pandits.findById(request.panditId())
                .filter(Pandit::isApproved)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Verified specialist not found"));
        Activity activity = activities.findById(request.activityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));
        Booking booking = bookings.save(new Booking(user.id(), pandit.getId(), activity.getId(),
            request.scheduledDate(), request.scheduledTime(), request.serviceAddress().trim(), activity.getBasePrice()));
        notifications.create(user.id(), "BOOKING", "Booking request received",
                "Your " + activity.getName() + " request has been sent to " + pandit.getFullName() + ".");
        if (pandit.getUserId() != null) {
            users.findById(pandit.getUserId()).ifPresent(account -> notifications.create(account.getId(), "BOOKING",
                "New booking request", "A new " + activity.getName() + " request is waiting for your response."));
        }
        BookingEventPublisher publisher = eventPublisher.getIfAvailable();
        if (publisher != null) publisher.publish(booking);
        sendSmsAfterCommit(booking, "A new booking request was received.");
        return response(booking);
    }

    @GetMapping("/mine")
    public List<BookingDashboardResponse> mine(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return bookings.findByUserIdOrderByCreatedAtDesc(user.id()).stream().map(this::dashboardResponse).toList();
    }

    @PatchMapping("/{bookingId}/cancel")
    @Transactional
    public BookingResponse cancel(@PathVariable Long bookingId, Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        Booking booking = bookings.findById(bookingId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!booking.getUserId().equals(user.id())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if ("PROCESSING".equals(booking.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wait for the active payment checkout to finish or expire before cancelling");
        }
        if ("PAID".equals(booking.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Paid bookings require support to review a refund");
        }
        if (!"PENDING".equals(booking.getBookingStatus()) && !"CONFIRMED".equals(booking.getBookingStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending or confirmed bookings can be cancelled");
        }
        booking.setBookingStatus("CANCELLED");
        Booking saved = bookings.save(booking);
        notifyPandit(saved, "Booking cancelled", "Booking " + saved.getId() + " was cancelled by the customer.");
        sendSmsAfterCommit(saved, "Your booking was cancelled.");
        return response(saved);
    }

    @PatchMapping("/{bookingId}/reschedule")
    @Transactional
    public BookingResponse reschedule(@PathVariable Long bookingId, @Valid @RequestBody RescheduleRequest request,
                                      Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        Booking booking = bookings.findById(bookingId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!booking.getUserId().equals(user.id())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if ("PROCESSING".equals(booking.getPaymentStatus()) || "PAID".equals(booking.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A booking with an active or completed payment cannot be rescheduled");
        }
        if (!"PENDING".equals(booking.getBookingStatus()) && !"CONFIRMED".equals(booking.getBookingStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending or confirmed bookings can be rescheduled");
        }
        if (request.scheduledDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking date must be today or later");
        }
        booking.rescheduleTo(request.scheduledDate(), request.scheduledTime());
        Booking saved = bookings.save(booking);
        notifyPandit(saved, "Booking rescheduled", "Booking " + saved.getId() + " was moved to " + saved.getScheduledDate() + ".");
        sendSmsAfterCommit(saved, "Your booking was rescheduled.");
        return response(saved);
    }

    @GetMapping("/pandit")
    public List<BookingDashboardResponse> forPandit(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        if (!"PANDIT".equals(user.role()) && !"ADMIN".equals(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Pandit pandit = pandits.findByUserId(user.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pandit profile not linked to this account"));
        return bookings.findByPanditIdOrderByScheduledDateAsc(pandit.getId()).stream().map(this::dashboardResponse).toList();
    }

    @PatchMapping("/{bookingId}/status")
    @Transactional
    public BookingResponse updateStatus(@PathVariable Long bookingId, @Valid @RequestBody StatusRequest request, Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        Booking booking = bookings.findById(bookingId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean admin = "ADMIN".equals(user.role());
        boolean assignedPandit = "PANDIT".equals(user.role()) && pandits.findById(booking.getPanditId())
                .map(pandit -> user.id().equals(pandit.getUserId())).orElse(false);
        if (!admin && !assignedPandit) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        String next = request.status().toUpperCase();
        if (next.equals("CANCELLED") && ("PROCESSING".equals(booking.getPaymentStatus()) || "PAID".equals(booking.getPaymentStatus()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An active or completed payment must be resolved before cancellation");
        }
        boolean allowed = switch (booking.getBookingStatus()) {
            case "PENDING" -> next.equals("CONFIRMED") || next.equals("CANCELLED");
            case "CONFIRMED" -> next.equals("IN_PROGRESS") || next.equals("CANCELLED");
            case "IN_PROGRESS" -> next.equals("COMPLETED");
            default -> false;
        };
        if (!allowed) throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid booking status transition");
        booking.setBookingStatus(next);
        Booking saved = bookings.save(booking);
        notifications.create(booking.getUserId(), "BOOKING", "Booking " + next.toLowerCase(),
                "Your booking " + booking.getId() + " is now " + next.toLowerCase().replace('_', ' ') + ".");
        sendSmsAfterCommit(saved, "Your booking status is now " + next.toLowerCase().replace('_', ' ') + ".");
        return response(saved);
    }

    private static BookingResponse response(Booking booking) {
        return new BookingResponse(booking.getId(), booking.getUserId(), booking.getPanditId(), booking.getActivityId(),
            booking.getScheduledDate(), booking.getScheduledTime(), booking.getServiceAddress(), booking.getTotalAmount(), booking.getCurrency(),
                booking.getBookingStatus(), booking.getPaymentStatus(), booking.getCreatedAt());
    }

    private BookingDashboardResponse dashboardResponse(Booking booking) {
        String userName = users.findById(booking.getUserId()).map(account -> account.getFullName()).orElse("Customer");
        String panditName = pandits.findById(booking.getPanditId()).map(Pandit::getFullName).orElse("Practitioner");
        String activityName = activities.findById(booking.getActivityId()).map(Activity::getName).orElse("Ceremony");
        return new BookingDashboardResponse(booking.getId(), booking.getUserId(), userName, booking.getPanditId(),
            panditName, booking.getActivityId(), activityName, booking.getScheduledDate(), booking.getScheduledTime(),
            booking.getServiceAddress(),
                booking.getTotalAmount(), booking.getCurrency(), booking.getBookingStatus(), booking.getPaymentStatus(),
                booking.getCreatedAt());
    }

    private void notifyPandit(Booking booking, String title, String message) {
        pandits.findById(booking.getPanditId()).map(Pandit::getUserId).filter(java.util.Objects::nonNull)
                .flatMap(users::findById)
                .ifPresent(account -> notifications.create(account.getId(), "BOOKING", title, message));
    }

    private void sendSmsAfterCommit(Booking booking, String message) {
        SmsService service = smsService.getIfAvailable();
        if (service != null) service.sendAfterCommit(booking.getId(), message);
    }

    public record CreateBookingRequest(@NotNull Long panditId, @NotNull Long activityId,
                                       @NotNull LocalDate scheduledDate, @NotNull LocalTime scheduledTime,
                                       @NotBlank String serviceAddress) { }
    public record StatusRequest(@NotBlank String status) { }
    public record RescheduleRequest(@NotNull LocalDate scheduledDate, @NotNull LocalTime scheduledTime) { }
    public record BookingResponse(Long id, Long userId, Long panditId, Long activityId, LocalDate scheduledDate,
                                  LocalTime scheduledTime,
                                  String serviceAddress, java.math.BigDecimal totalAmount, String currency,
                                  String bookingStatus, String paymentStatus, java.time.Instant createdAt) { }
    public record BookingDashboardResponse(Long id, Long userId, String userName, Long panditId, String panditName,
                                           Long activityId, String activityName, LocalDate scheduledDate,
                                           LocalTime scheduledTime, String serviceAddress, java.math.BigDecimal totalAmount, String currency,
                                           String bookingStatus, String paymentStatus, java.time.Instant createdAt) { }
}
