-- Добавляем колонку филиала в таблицу ресурсов (кабинетов)
ALTER TABLE resources ADD COLUMN IF NOT EXISTS branch_id TEXT;

-- Если есть старые ресурсы без филиала, можно привязать их к первому найденному (опционально)
-- UPDATE resources SET branch_id = (SELECT id FROM branches LIMIT 1) WHERE branch_id IS NULL;
