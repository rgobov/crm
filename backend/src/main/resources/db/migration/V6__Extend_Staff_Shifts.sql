-- Расширяем график работы мастеров на весь февраль и март 2026 года
DO $$
DECLARE
    staff_id_val TEXT;
    target_date DATE;
    i INTEGER;
BEGIN
    FOR i IN 1..25 LOOP
        staff_id_val := 'staff-' || i;

        -- Используем generate_series для генерации диапазона дат (корректный способ для Postgres)
        FOR target_date IN
            SELECT generate_series('2026-02-01'::date, '2026-03-31'::date, '1 day'::interval)::date
        LOOP

            -- Проверяем, существует ли уже смена на эту дату
            IF NOT EXISTS (SELECT 1 FROM staff_shifts WHERE staff_id = staff_id_val AND date = target_date) THEN
                INSERT INTO staff_shifts (id, staff_id, date, work_start_time, work_end_time, is_day_off, tenant_id)
                VALUES (
                    gen_random_uuid(),
                    staff_id_val,
                    target_date,
                    (CASE WHEN i % 2 = 0 THEN '09:00:00' ELSE '10:00:00' END)::TIME,
                    (CASE WHEN i % 2 = 0 THEN '18:00:00' ELSE '21:00:00' END)::TIME,
                    false,
                    'tenant-1'
                );
            END IF;
        END LOOP;
    END LOOP;
END $$;
