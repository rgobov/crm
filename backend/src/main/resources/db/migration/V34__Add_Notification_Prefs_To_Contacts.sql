-- Add notification preference fields to contacts
ALTER TABLE contacts ADD COLUMN notification_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE contacts ADD COLUMN notification_lead_time_hours INTEGER NOT NULL DEFAULT 24;
