package com.onlinepandit.notification;

import com.onlinepandit.booking.Booking;
import com.onlinepandit.booking.BookingRepository;
import com.onlinepandit.identity.UserAccount;
import com.onlinepandit.identity.UserAccountRepository;
import com.onlinepandit.marketplace.ActivityRepository;
import com.onlinepandit.marketplace.Pandit;
import com.onlinepandit.marketplace.PanditRepository;
import com.twilio.Twilio;
import com.twilio.exception.TwilioException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.sms.enabled", havingValue = "true")
public class SmsService {
    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);
    private final BookingRepository bookings;
    private final UserAccountRepository users;
    private final PanditRepository pandits;
    private final ActivityRepository activities;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    public SmsService(BookingRepository bookings, UserAccountRepository users, PanditRepository pandits,
                      ActivityRepository activities, @Value("${app.sms.twilio.account-sid:}") String accountSid,
                      @Value("${app.sms.twilio.auth-token:}") String authToken,
                      @Value("${app.sms.twilio.from-number:}") String fromNumber) {
        this.bookings = bookings;
        this.users = users;
        this.pandits = pandits;
        this.activities = activities;
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
    }

    public void sendAfterCommit(Long bookingId, String update) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { sendBookingUpdate(bookingId, update); }
            });
        } else {
            sendBookingUpdate(bookingId, update);
        }
    }

    public boolean sendReminder(Long bookingId) {
        Booking booking = bookings.findByIdForUpdate(bookingId).orElse(null);
        if (booking == null || booking.getReminderSentAt() != null) return true;
        if (!"CONFIRMED".equals(booking.getBookingStatus())) return true;
        boolean successful = sendToBookingParties(booking, reminderMessage(booking));
        if (successful) {
            booking.markReminderSent();
            bookings.save(booking);
        }
        return successful;
    }

    private void sendBookingUpdate(Long bookingId, String update) {
        bookings.findById(bookingId).ifPresent(booking -> sendToBookingParties(booking, updateMessage(booking, update)));
    }

    private boolean sendToBookingParties(Booking booking, String body) {
        if (accountSid.isBlank() || authToken.isBlank() || fromNumber.isBlank()) {
            logger.warn("Twilio SMS enabled without complete credentials; skipped booking {}", booking.getId());
            return false;
        }
        Twilio.init(accountSid, authToken);
        List<String> recipients = new ArrayList<>();
        users.findById(booking.getUserId()).filter(UserAccount::isSmsOptIn).map(UserAccount::getPhone)
            .filter(SmsService::isE164).ifPresent(recipients::add);
        pandits.findById(booking.getPanditId()).map(Pandit::getUserId).filter(java.util.Objects::nonNull)
            .flatMap(users::findById).filter(UserAccount::isSmsOptIn).map(UserAccount::getPhone)
            .filter(SmsService::isE164).ifPresent(recipients::add);
        boolean sent = true;
        for (String recipient : recipients.stream().distinct().toList()) {
            try {
                Message.creator(new PhoneNumber(recipient), new PhoneNumber(fromNumber), body).create();
            } catch (TwilioException exception) {
                sent = false;
                logger.error("Twilio could not send booking update for booking {}", booking.getId(), exception);
            }
        }
        return sent;
    }

    private String updateMessage(Booking booking, String update) {
        String activity = activities.findById(booking.getActivityId()).map(value -> value.getName()).orElse("your ceremony");
        return "OnlinePandit: " + update + " for " + activity + " booking #" + booking.getId() +
                " on " + booking.getScheduledDate() + " at " + booking.getScheduledTime() + ".";
    }

    private String reminderMessage(Booking booking) {
        return updateMessage(booking, "Reminder: your confirmed booking is tomorrow");
    }

    private static boolean isE164(String phone) { return phone != null && phone.matches("\\+[1-9][0-9]{7,14}"); }
}
