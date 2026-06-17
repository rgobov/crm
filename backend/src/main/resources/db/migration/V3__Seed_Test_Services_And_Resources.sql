-- Наполнение тестовыми услугами для tenant-1
INSERT INTO services (id, name, duration_in_minutes, tenant_id)
VALUES
('svc-1', 'Классический маникюр', 45, 'tenant-1'),
('svc-2', 'Аппаратный педикюр', 60, 'tenant-1'),
('svc-3', 'Стрижка мужская', 30, 'tenant-1'),
('svc-4', 'Стрижка женская (длинные волосы)', 90, 'tenant-1'),
('svc-5', 'Окрашивание в один тон', 120, 'tenant-1'),
('svc-6', 'Массаж лица', 40, 'tenant-1')
ON CONFLICT DO NOTHING;

-- Наполнение тестовыми ресурсами для tenant-1
INSERT INTO resources (id, name, description, tenant_id)
VALUES
('res-1', 'Кабинет №1', 'Маникюрный зал', 'tenant-1'),
('res-2', 'Кабинет №2', 'Педикюрный зал', 'tenant-1'),
('res-3', 'VIP Кабинет', 'Повышенный комфорт', 'tenant-1'),
('res-4', 'Рабочее место №1', 'Главный зал', 'tenant-1'),
('res-5', 'Рабочее место №2', 'Главный зал', 'tenant-1')
ON CONFLICT DO NOTHING;
