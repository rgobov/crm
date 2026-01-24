-- Мы используем V6, так как Flyway на сервере мог "застрять" на V4
-- Принудительно обновляем пароли для всех пользователей на 'qwerty'
-- Валидный хеш (ровно 60 символов)
UPDATE users
SET password = '$2a$10$XFMpS9H6xvNPKgSVv.uGxeSJVWJzpy07xd00DMxs.7u41W3uy.G'
WHERE email LIKE 'forts%';
