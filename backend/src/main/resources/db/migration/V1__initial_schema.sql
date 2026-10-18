CREATE TABLE user_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(190) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    city VARCHAR(80),
    country VARCHAR(80),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE activities (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    category VARCHAR(80) NOT NULL,
    duration VARCHAR(60) NOT NULL,
    base_price DECIMAL(10,2) NOT NULL,
    religion VARCHAR(60) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE pandits (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    years_experience INT NOT NULL,
    date_of_birth DATE,
    city VARCHAR(80) NOT NULL,
    region VARCHAR(80),
    country VARCHAR(80) NOT NULL,
    religion VARCHAR(60) NOT NULL,
    languages VARCHAR(255),
    specialties VARCHAR(500),
    image_url VARCHAR(500),
    rating DECIMAL(2,1) NOT NULL DEFAULT 5.0,
    review_count INT NOT NULL DEFAULT 0,
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    user_id BIGINT UNIQUE,
    CONSTRAINT fk_pandit_user FOREIGN KEY (user_id) REFERENCES user_accounts(id),
    INDEX idx_pandit_region (country, region, city, approved)
);

CREATE TABLE bookings (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    pandit_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    scheduled_date DATE NOT NULL,
    service_address VARCHAR(500) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    booking_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_booking_pandit FOREIGN KEY (pandit_id) REFERENCES pandits(id),
    CONSTRAINT fk_booking_activity FOREIGN KEY (activity_id) REFERENCES activities(id),
    INDEX idx_booking_user_date (user_id, scheduled_date),
    INDEX idx_booking_pandit_date (pandit_id, scheduled_date)
);

CREATE TABLE feedback (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    pandit_id BIGINT NOT NULL,
    rating TINYINT NOT NULL,
    comment VARCHAR(1000),
    moderated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedback_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_feedback_pandit FOREIGN KEY (pandit_id) REFERENCES pandits(id),
    CONSTRAINT chk_feedback_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category VARCHAR(30) NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES user_accounts(id),
    INDEX idx_notification_user_read (user_id, read_at, created_at)
);

INSERT INTO activities (name, category, duration, base_price, religion, active) VALUES
('Griha Pravesh', 'House & family', '3–4 hours', 6500.00, 'Hindu', TRUE),
('Wedding ceremony', 'Life celebrations', 'Full day', 15000.00, 'Hindu', TRUE),
('Satyanarayan Puja', 'Puja & havan', '2–3 hours', 4500.00, 'Hindu', TRUE),
('Havan', 'Puja & havan', '2 hours', 3500.00, 'Hindu', TRUE),
('Naming ceremony', 'Life celebrations', '2 hours', 4000.00, 'Hindu', TRUE),
('Memorial service', 'Life celebrations', 'By arrangement', 5000.00, 'Multi-faith', TRUE);

INSERT INTO pandits (full_name, years_experience, city, region, country, religion, languages, specialties, image_url, rating, review_count, approved) VALUES
('Pandit Raghav Sharma', 18, 'Mumbai', 'Maharashtra', 'India', 'Hindu', 'Hindi, Sanskrit, Marathi', 'Griha Pravesh, Vedic havan', 'https://images.unsplash.com/photo-1604881991720-f91add269bed?auto=format&fit=crop&w=900&q=85', 4.9, 128, TRUE),
('Acharya Devendra Joshi', 24, 'Mumbai', 'Maharashtra', 'India', 'Hindu', 'Hindi, Sanskrit, Gujarati', 'Wedding rituals, Puja', 'https://images.unsplash.com/photo-1617137968427-85924c800a22?auto=format&fit=crop&w=900&q=85', 4.8, 94, TRUE),
('Pandit Kiran Mishra', 15, 'Mumbai', 'Maharashtra', 'India', 'Hindu', 'Hindi, Sanskrit, English', 'Havan, Griha Pravesh', 'https://images.unsplash.com/photo-1531123897727-8f129e1688ce?auto=format&fit=crop&w=900&q=85', 4.9, 76, TRUE),
('Acharya Anil Dubey', 20, 'Delhi', 'Delhi', 'India', 'Hindu', 'Hindi, Sanskrit', 'Wedding rituals, Havan', 'https://images.unsplash.com/photo-1604881991720-f91add269bed?auto=format&fit=crop&w=900&q=85', 4.7, 63, TRUE);
