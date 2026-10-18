ALTER TABLE bookings
    ADD COLUMN scheduled_time TIME NOT NULL DEFAULT '09:00:00',
    ADD COLUMN reminder_sent_at TIMESTAMP NULL;