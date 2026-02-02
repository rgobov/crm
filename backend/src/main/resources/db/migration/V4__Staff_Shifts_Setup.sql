-- Настройка графиков работы для тестовых мастеров (staff-1...staff-25)
-- Заполняем на сегодня и ближайшие 7 дней
DO $$
DECLARE
    staff_id_val TEXT;
    target_date DATE;
    i INTEGER;
    d INTEGER;
BEGIN
    FOR i IN 1..25 LOOP
        staff_id_val := 'staff-' || i;

        FOR d IN 0..7 LOOP
            target_date := CURRENT_DATE + d;

            -- Добавляем явное приведение к ::TIME
            INSERT INTO staff_shifts (id, staff_id, date, work_start_time, work_end_time, break_start_time, break_end_time, is_day_off, tenant_id)
            VALUES (
                gen_random_uuid(),
                staff_id_val,
                target_date,
                (CASE WHEN i % 2 = 0 THEN '09:00:00' ELSE '10:00:00' END)::TIME,
                (CASE WHEN i % 2 = 0 THEN '18:00:00' ELSE '20:00:00' END)::TIME,
                '13:00:00'::TIME, '14:00:00'::TIME,
                CASE WHEN (i % 5 = 0 AND EXTRACT(DOW FROM target_date) = 6) THEN true ELSE false END,
                'tenant-1'
            ) ON CONFLICT DO NOTHING;
        END LOOP;
    END LOOP;
END $$;
