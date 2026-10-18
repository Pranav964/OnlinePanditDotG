package com.onlinepandit.notification;

import com.onlinepandit.security.JwtAuthenticationFilter.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notifications;
    private final NotificationRepository repository;
    public NotificationController(NotificationService notifications, NotificationRepository repository) {
        this.notifications = notifications;
        this.repository = repository;
    }

    @GetMapping
    public NotificationService.NotificationPage list(Authentication authentication,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination values");
        return notifications.list(principal(authentication).id(), unreadOnly, category, page, size);
    }

    @PatchMapping("/{id}/read")
    public Notification markRead(@PathVariable Long id, Authentication authentication) {
        Notification item = repository.findById(id).filter(value -> value.getUserId().equals(principal(authentication).id()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        item.markRead();
        return repository.save(item);
    }

    @PostMapping("/read-all")
    public ReadAllResponse markAllRead(Authentication authentication) {
        return new ReadAllResponse(notifications.markAllRead(principal(authentication).id()));
    }

    private static UserPrincipal principal(Authentication authentication) { return (UserPrincipal) authentication.getPrincipal(); }
    public record ReadAllResponse(int updated) { }
}
