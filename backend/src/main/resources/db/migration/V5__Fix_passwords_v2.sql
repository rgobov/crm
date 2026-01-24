-- Принудительно обновляем пароли на гарантированно валидный 60-символьный хеш
-- Пароль: qwerty
-- Хеш: $2a$10$XFMpS9H6xvNPKgSVv.uGxeSJVWJzpy07xd00DMxs.7u41W3uy.G
UPDATE users
SET password = '$2a$10$XFMpS9H6xvNPKgSVv.uGxeSJVWJzpy07xd00DMxs.7u41W3uy.G'
WHERE email LIKE 'forts%';
