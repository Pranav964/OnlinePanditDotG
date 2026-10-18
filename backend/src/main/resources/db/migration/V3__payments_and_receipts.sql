CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    provider VARCHAR(24) NOT NULL,
    provider_session_id VARCHAR(190),
    provider_payment_id VARCHAR(190),
    checkout_url VARCHAR(1000),
    amount DECIMAL(10,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    payment_status VARCHAR(24) NOT NULL DEFAULT 'CREATED',
    receipt_number VARCHAR(40) UNIQUE,
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES user_accounts(id),
    INDEX idx_payment_status_created (payment_status, created_at)
);