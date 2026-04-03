-- Добавление колонки для аватара мастера
ALTER TABLE staff_members ADD COLUMN IF NOT EXISTS photo_url TEXT;
