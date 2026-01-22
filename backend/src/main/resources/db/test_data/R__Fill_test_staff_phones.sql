-- Этот скрипт заполняет тестовые телефоны.
-- Он запускается ТОЛЬКО если мы укажем Flyway эту папку.
UPDATE staff_members
SET phone = '791200000' || lpad(floor(random() * 99)::text, 2, '0')
WHERE phone IS NULL;
