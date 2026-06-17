CREATE TABLE telegram_settings (
    tenant_id VARCHAR(255) PRIMARY KEY,
    connected_phone VARCHAR(50),
    is_active BOOLEAN DEFAULT FALSE,
    connected_at TIMESTAMP,
    CONSTRAINT fk_telegram_company FOREIGN KEY (tenant_id) REFERENCES companies(id) ON DELETE CASCADE
);
