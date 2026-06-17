-- Добавляем колонку филиала в таблицу смен сотрудников
ALTER TABLE staff_shifts ADD COLUMN IF NOT EXISTS branch_id TEXT;

-- Обновляем существующие смены (привязываем их к первому филиалу тенанта для миграции данных)
UPDATE staff_shifts s
SET branch_id = (SELECT b.id FROM branches b WHERE b.tenant_id = s.tenant_id LIMIT 1)
WHERE s.branch_id IS NULL;
