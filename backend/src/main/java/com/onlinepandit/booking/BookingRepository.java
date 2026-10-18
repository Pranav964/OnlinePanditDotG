package com.onlinepandit.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.time.LocalDate;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Booking> findByPanditIdOrderByScheduledDateAsc(Long panditId);

    @Query("select b from Booking b where b.scheduledDate = :scheduledDate and b.bookingStatus = 'CONFIRMED' and b.reminderSentAt is null")
    List<Booking> findReminderCandidates(@Param("scheduledDate") LocalDate scheduledDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    java.util.Optional<Booking> findByIdForUpdate(@Param("id") Long id);
}
