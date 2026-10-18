package com.onlinepandit.booking;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class BookingEventPublisher {
    private final KafkaTemplate<String, Object> kafka;
    public BookingEventPublisher(KafkaTemplate<String, Object> kafka) { this.kafka = kafka; }
    public void publish(Booking booking) {
        Runnable send = () -> kafka.send("booking-events", booking.getId().toString(), new BookingCreatedEvent(
                booking.getId(), booking.getUserId(), booking.getPanditId(), booking.getActivityId(), Instant.now()));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { send.run(); }
            });
            return;
        }
        send.run();
    }
    public record BookingCreatedEvent(Long bookingId, Long userId, Long panditId, Long activityId, Instant occurredAt) { }
}
