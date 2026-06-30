CREATE TABLE user_consents (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id),
    consent_type VARCHAR(50) NOT NULL DEFAULT 'personal_data',
    policy_version VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    accepted_at TIMESTAMP NOT NULL DEFAULT now(),
    revoked_at TIMESTAMP
);

CREATE INDEX idx_user_consents_user_id ON user_consents(user_id);
CREATE UNIQUE INDEX idx_user_consents_active ON user_consents(user_id, consent_type) WHERE revoked_at IS NULL;
