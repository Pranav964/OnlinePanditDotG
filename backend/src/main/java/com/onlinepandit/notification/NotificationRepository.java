package com.onlinepandit.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("select n from Notification n where n.userId = :userId and (:unreadOnly = false or n.readAt is null) and (:category is null or lower(n.category) = lower(:category)) order by n.createdAt desc")
    Page<Notification> search(@Param("userId") Long userId, @Param("unreadOnly") boolean unreadOnly,
                              @Param("category") String category, Pageable pageable);
    long countByUserIdAndReadAtIsNull(Long userId);
    @Modifying
    @Query("update Notification n set n.readAt = :readAt where n.userId = :userId and n.readAt is null")
    int markAllRead(@Param("userId") Long userId, @Param("readAt") Instant readAt);
}
