-- Добавляем колонку для хранения времени напоминания в часах
ALTER TABLE notification_templates ADD COLUMN lead_time_hours INTEGER;

-- Очищаем старые типы шаблонов, которые мы решили не использовать
DELETE FROM notification_templates WHERE type = 'APPOINTMENT_CONFIRMATION';
DELETE FROM notification_templates WHERE type = 'APPOINTMENT_CANCELLED';
