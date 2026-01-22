-- В этой миграции ТОЛЬКО изменение структуры. Это безопасно для продакшена.
ALTER TABLE staff_members ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
