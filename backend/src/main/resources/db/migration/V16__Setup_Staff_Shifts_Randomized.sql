-- Очистка и создание рандомизированных смен для всех 50 мастеров
DO $$
DECLARE
    staff_record RECORD;
    target_date DATE;
    start_time TIME;
    end_time TIME;
    break_start TIME;
    break_end TIME;
    d INTEGER;
BEGIN
    DELETE FROM staff_shifts WHERE tenant_id = 'tenant-1';

    FOR staff_record IN SELECT id FROM staff_members WHERE tenant_id = 'tenant-1' LOOP
        FOR d IN 0..14 LOOP
            target_date := CURRENT_DATE + d;
            start_time := ('05:00:00'::TIME + (floor(random() * 10) || ' hours')::INTERVAL);
            end_time := (start_time + interval '12 hours')::TIME;
            break_start := (start_time + interval '5 hours')::TIME;
            break_end := (break_start + interval '1 hour')::TIME;

            INSERT INTO staff_shifts (
                id, staff_id, date,
                work_start_time, work_end_time,
                break_start_time, break_end_time,
                is_day_off, tenant_id
            )
            VALUES (
                gen_random_uuid(),
                staff_record.id,
                target_date,
                start_time,
                end_time,
                break_start,
                break_end,
                CASE WHEN (EXTRACT(DOW FROM target_date) IN (0, 6) AND random() > 0.5) THEN true ELSE false END,
                'tenant-1'
            );
        END LOOP;
    END LOOP;
END $$;
