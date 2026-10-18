ALTER TABLE user_accounts
    ADD COLUMN sms_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN sms_opt_in_updated_at TIMESTAMP NULL;