-- Восстановление миграции после отката Git.
-- Колонка client_phone уже может существовать в БД.
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS client_phone VARCHAR(255);
