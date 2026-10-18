package com.onlinepandit.notification;

import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final SimpMessagingTemplate messaging;

    public NotificationService(NotificationRepository notifications, SimpMessagingTemplate messaging) {
        this.notifications = notifications;
        this.messaging = messaging;
    }

    @Transactional
    public Notification create(Long userId, String category, String title, String message) {
        Notification notification = notifications.save(new Notification(userId, category, title, message));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messaging.convertAndSendToUser(userId.toString(), "/queue/notifications", notification);
                }
            });
        } else {
            messaging.convertAndSendToUser(userId.toString(), "/queue/notifications", notification);
        }
        return notification;
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notifications.markAllRead(userId, Instant.now());
    }

    public NotificationPage list(Long userId, boolean unreadOnly, String category, int page, int size) {
        var pageable = PageRequest.of(page, size);
        String normalizedCategory = category == null || category.isBlank() ? null : category.trim();
        var result = notifications.search(userId, unreadOnly, normalizedCategory, pageable);
        return new NotificationPage(result.getContent(), notifications.countByUserIdAndReadAtIsNull(userId),
            result.getNumber(), result.getTotalPages());
    }

    public record NotificationPage(java.util.List<Notification> content, long unreadCount, int page, int totalPages) { }
}
