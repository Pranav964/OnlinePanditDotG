package com.onlinepandit.admin;

import com.onlinepandit.booking.BookingController.BookingResponse;
import com.onlinepandit.booking.BookingRepository;
import com.onlinepandit.identity.UserAccountRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final BookingRepository bookings;
    private final UserAccountRepository users;
    public AdminController(BookingRepository bookings, UserAccountRepository users) {
        this.bookings = bookings;
        this.users = users;
    }

    @GetMapping("/bookings")
    public List<BookingResponse> bookings() {
        return bookings.findAll().stream().map(booking -> new BookingResponse(booking.getId(), booking.getUserId(),
                booking.getPanditId(), booking.getActivityId(), booking.getScheduledDate(), booking.getServiceAddress(),
                booking.getTotalAmount(), booking.getCurrency(), booking.getBookingStatus(), booking.getPaymentStatus(),
                booking.getCreatedAt())).toList();
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users() {
        return users.findAll().stream().map(user -> new AdminUserResponse(user.getId(), user.getFullName(), user.getEmail(),
                user.getPhone(), user.getCity(), user.getCountry(), user.getRole().name(), user.getCreatedAt())).toList();
    }

    public record AdminUserResponse(Long id, String fullName, String email, String phone, String city,
                                    String country, String role, java.time.Instant createdAt) { }
}
