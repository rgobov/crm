-- Привязываем все старые записи (где branch_id IS NULL) к филиалу "Супер"
-- Это необходимо для того, чтобы они начали отображаться в календаре и таймлайне филиала
UPDATE appointments
SET branch_id = 'br-super'
WHERE branch_id IS NULL;

-- Также привяжем ресурсы (кабинеты), если они были созданы ранее
UPDATE resources
SET branch_id = 'br-super'
WHERE branch_id IS NULL;
