-- Наполнение мастерами до 50 и распределение по филиалам "Супер" и "Виртуальный"
DO $$
DECLARE
    super_id VARCHAR(255);
    virtual_id VARCHAR(255);
    target_date DATE;
    i INTEGER;
    d INTEGER;
BEGIN
    -- 1. Получаем ID филиалов по названиям
    SELECT id INTO super_id FROM branches WHERE name = 'Супер' LIMIT 1;
    SELECT id INTO virtual_id FROM branches WHERE name = 'Виртуальный' LIMIT 1;

    -- Если филиалы не найдены (на всякий случай, чтобы миграция не упала)
    IF super_id IS NULL OR virtual_id IS NULL THEN
        RAISE NOTICE 'One of the branches not found. Seeding skipped.';
        RETURN;
    END IF;

    -- 2. Добавляем еще 25 мастеров (от 26 до 50)
    FOR i IN 26..50 LOOP
        INSERT INTO staff_members (id, name, specialty, tenant_id, active, phone)
        VALUES ('staff-' || i, 'Мастер ' || i, 'Специалист', 'tenant-1', true, '+7900' || LPAD(i::text, 7, '0'))
        ON CONFLICT DO NOTHING;

        INSERT INTO users (id, email, password, role, staff_id, tenant_id)
        VALUES ('user-staff-' || i, 'forts' || i || '@e1.ru', '$2a$10$XFMpS9H6xvNPKgSVv.uGxeSJVWJzpy07xd00DMxs.7u41W3uy.G', 'EMPLOYEE', 'staff-' || i, 'tenant-1')
        ON CONFLICT DO NOTHING;
    END LOOP;

    -- 3. Распределяем по филиалам (ManyToMany)
    -- Мастера 1-25 -> Филиал "Супер"
    FOR i IN 1..25 LOOP
        INSERT INTO staff_member_branches (staff_member_id, branch_id)
        VALUES ('staff-' || i, super_id)
        ON CONFLICT DO NOTHING;
    END LOOP;

    -- Мастера 26-50 -> Филиал "Виртуальный"
    FOR i IN 26..50 LOOP
        INSERT INTO staff_member_branches (staff_member_id, branch_id)
        VALUES ('staff-' || i, virtual_id)
        ON CONFLICT DO NOTHING;
    END LOOP;

    -- Тестовые "универсалы" (в оба филиала)
    INSERT INTO staff_member_branches (staff_member_id, branch_id) VALUES ('staff-1', virtual_id) ON CONFLICT DO NOTHING;
    INSERT INTO staff_member_branches (staff_member_id, branch_id) VALUES ('staff-50', super_id) ON CONFLICT DO NOTHING;

    -- 4. Создаем смены для новых мастеров (26-50) на 7 дней
    FOR i IN 26..50 LOOP
        FOR d IN 0..7 LOOP
            target_date := CURRENT_DATE + d;
            INSERT INTO staff_shifts (id, staff_id, date, work_start_time, work_end_time, break_start_time, break_end_time, is_day_off, tenant_id)
            VALUES (
                gen_random_uuid(),
                'staff-' || i,
                target_date,
                (CASE WHEN i % 2 = 0 THEN '08:00:00' ELSE '11:00:00' END)::TIME,
                (CASE WHEN i % 2 = 0 THEN '17:00:00' ELSE '21:00:00' END)::TIME,
                '14:00:00'::TIME, '15:00:00'::TIME,
                CASE WHEN (i % 3 = 0 AND EXTRACT(DOW FROM target_date) = 0) THEN true ELSE false END,
                'tenant-1'
            ) ON CONFLICT DO NOTHING;
        END LOOP;
    END LOOP;

END $$;
