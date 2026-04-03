-- 1. Включаем расширение для шифрования (если еще нет)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 2. Принудительно обновляем пароль админа на 'qwerty' через функцию базы
UPDATE users
SET password = crypt('qwerty', gen_salt('bf', 10))
WHERE email = 'forts@e1.ru';

-- 3. Принудительно обновляем пароли всех сотрудников на 'qwerty'
UPDATE users
SET password = crypt('qwerty', gen_salt('bf', 10))
WHERE email LIKE 'forts%@e1.ru';
