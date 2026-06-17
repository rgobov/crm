-- Устанавливаем ограничение NOT NULL для поля branch_id
-- Это гарантирует, что ни одна запись или ресурс не останутся без привязки к филиалу

-- 1. Для таблицы записей
ALTER TABLE appointments ALTER COLUMN branch_id SET NOT NULL;

-- 2. Для таблицы ресурсов (кабинетов)
ALTER TABLE resources ALTER COLUMN branch_id SET NOT NULL;
