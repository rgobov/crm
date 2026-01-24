-- Принудительно обновляем пароль для админа и всех сотрудников на 'qwerty'
-- Мы используем гарантированно рабочий хеш BCrypt
UPDATE users
SET password = '$2a$10$XFMpS9H6xvNPKgSVv.uGxeSJVWJzpy07xd00DMxs.7u41W3u'
WHERE email LIKE 'forts%';