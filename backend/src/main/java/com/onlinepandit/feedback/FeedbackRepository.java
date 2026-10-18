package com.onlinepandit.feedback;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    boolean existsByBookingId(Long bookingId);
    List<Feedback> findByPanditIdAndModeratedTrueOrderByCreatedAtDesc(Long panditId);
    List<Feedback> findByModeratedFalseOrderByCreatedAtAsc();
}
