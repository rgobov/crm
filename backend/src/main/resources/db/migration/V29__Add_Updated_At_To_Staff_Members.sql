ALTER TABLE staff_members ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Обновляем существующие записи, чтобы у них было начальное значение
UPDATE staff_members SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;
