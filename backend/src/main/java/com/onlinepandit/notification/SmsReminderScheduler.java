package com.onlinepandit.notification;

import com.onlinepandit.booking.BookingRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@ConditionalOnExpression("${app.sms.enabled:false} && ${app.sms.reminders-enabled:false}")
public class SmsReminderScheduler {
    private final BookingRepository bookings;
    private final SmsService sms;
    private final ZoneId zoneId;

    public SmsReminderScheduler(BookingRepository bookings, SmsService sms,
                                @org.springframework.beans.factory.annotation.Value("${app.sms.reminder-zone:UTC}") String zone) {
        this.bookings = bookings;
        this.sms = sms;
        this.zoneId = ZoneId.of(zone);
    }

    @Scheduled(cron = "${app.sms.reminder-cron:0 */15 8-20 * * *}", zone = "${app.sms.reminder-zone:UTC}")
    @Transactional
    public void sendTomorrowReminders() {
        LocalDate tomorrow = LocalDate.now(zoneId).plusDays(1);
        bookings.findReminderCandidates(tomorrow).forEach(booking -> sms.sendReminder(booking.getId()));
    }
}
