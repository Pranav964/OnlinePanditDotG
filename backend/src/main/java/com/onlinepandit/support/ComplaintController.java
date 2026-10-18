package com.onlinepandit.support;

import com.onlinepandit.booking.BookingRepository;
import com.onlinepandit.notification.NotificationService;
import com.onlinepandit.payment.PaymentRepository;
import com.onlinepandit.payment.PaymentTransaction;
import com.onlinepandit.security.JwtAuthenticationFilter.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class ComplaintController {
    private static final Set<String> STATUSES = Set.of("OPEN", "IN_REVIEW", "RESOLVED");
    private final ComplaintRepository complaints;
    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final NotificationService notifications;

    public ComplaintController(ComplaintRepository complaints, BookingRepository bookings, PaymentRepository payments,
                               NotificationService notifications) {
        this.complaints = complaints;
        this.bookings = bookings;
        this.payments = payments;
        this.notifications = notifications;
    }

    @PostMapping("/complaints")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ComplaintResponse create(@Valid @RequestBody CreateComplaintRequest request, Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        if (request.bookingId() != null) {
            bookings.findById(request.bookingId()).filter(booking -> booking.getUserId().equals(user.id()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        }
        if (request.paymentId() != null) {
            payments.findById(request.paymentId()).filter(payment -> payment.getUserId().equals(user.id()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        }
        if (request.bookingId() != null && request.paymentId() != null) {
            PaymentTransaction payment = payments.findById(request.paymentId()).orElseThrow();
            if (!payment.getBookingId().equals(request.bookingId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment does not belong to this booking");
            }
        }
        Complaint complaint = complaints.save(new Complaint(user.id(), request.bookingId(), request.paymentId(),
                request.subject().trim(), request.description().trim()));
        return response(complaint);
    }

    @GetMapping("/complaints/mine")
    public List<ComplaintResponse> mine(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return complaints.findByUserIdOrderByCreatedAtDesc(user.id()).stream().map(ComplaintController::response).toList();
    }

    @GetMapping("/admin/complaints")
    public List<ComplaintResponse> all() {
        return complaints.findAllByOrderByCreatedAtDesc().stream().map(ComplaintController::response).toList();
    }

    @PatchMapping("/admin/complaints/{id}")
    @Transactional
    public ComplaintResponse update(@PathVariable Long id, @Valid @RequestBody UpdateComplaintRequest request) {
        String status = request.status().toUpperCase();
        if (!STATUSES.contains(status)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be OPEN, IN_REVIEW, or RESOLVED");
        Complaint complaint = complaints.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        complaint.updateStatus(status, request.adminNote() == null ? null : request.adminNote().trim());
        Complaint saved = complaints.save(complaint);
        notifications.create(saved.getUserId(), "SUPPORT", "Support request updated",
                "Your support request " + saved.getId() + " is now " + status.toLowerCase().replace('_', ' ') + ".");
        return response(saved);
    }

    private static ComplaintResponse response(Complaint complaint) {
        return new ComplaintResponse(complaint.getId(), complaint.getUserId(), complaint.getBookingId(), complaint.getPaymentId(),
                complaint.getSubject(), complaint.getDescription(), complaint.getStatus(), complaint.getAdminNote(),
                complaint.getCreatedAt(), complaint.getUpdatedAt());
    }

    public record CreateComplaintRequest(Long bookingId, Long paymentId, @NotBlank @Size(max = 160) String subject,
                                         @NotBlank @Size(max = 2000) String description) { }
    public record UpdateComplaintRequest(@NotBlank String status, @Size(max = 1000) String adminNote) { }
    public record ComplaintResponse(Long id, Long userId, Long bookingId, Long paymentId, String subject,
                                    String description, String status, String adminNote, Instant createdAt, Instant updatedAt) { }
}
