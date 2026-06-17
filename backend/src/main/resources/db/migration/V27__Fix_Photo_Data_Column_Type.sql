-- Безопасная миграция данных из OID в BYTEA без потери существующих фото
DO $$
BEGIN
    -- Проверяем, является ли колонка типом 'oid'
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'staff_members'
        AND column_name = 'photo_data'
        AND data_type = 'oid'
    ) THEN
        -- Используем lo_get для извлечения данных из Large Objects в массив байтов
        ALTER TABLE staff_members ALTER COLUMN photo_data TYPE BYTEA USING lo_get(photo_data);
    END IF;
END $$;
