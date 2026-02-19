-- Добавляем поле для объекта визита (машина, номер и т.д.)
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS reference_tag TEXT;

-- Добавляем массив тегов для клиентов
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS tags TEXT[];
