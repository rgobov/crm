-- Гарантированное создание начальных филиалов
INSERT INTO branches (id, name, address, timezone, tenant_id)
VALUES
    ('br-super', 'Супер', 'Основной адрес', 'Europe/Moscow', 'tenant-1'),
    ('br-virtual', 'Виртуальный', 'Онлайн / Удаленно', 'Europe/Moscow', 'tenant-1')
ON CONFLICT (id) DO NOTHING;

-- Привязка мастеров
INSERT INTO staff_member_branches (staff_member_id, branch_id)
SELECT id, 'br-super' FROM staff_members
WHERE id LIKE 'staff-%' AND CAST(SUBSTRING(id FROM 7) AS INTEGER) <= 25
ON CONFLICT DO NOTHING;

INSERT INTO staff_member_branches (staff_member_id, branch_id)
SELECT id, 'br-virtual' FROM staff_members
WHERE id LIKE 'staff-%' AND CAST(SUBSTRING(id FROM 7) AS INTEGER) > 25
ON CONFLICT DO NOTHING;
