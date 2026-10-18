CREATE TABLE complaints (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    booking_id BIGINT NULL,
    payment_id BIGINT NULL,
    subject VARCHAR(160) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'OPEN',
    admin_note VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_complaint_user FOREIGN KEY (user_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_complaint_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_complaint_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    INDEX idx_complaint_status_created (status, created_at),
    INDEX idx_complaint_user_created (user_id, created_at)
);
