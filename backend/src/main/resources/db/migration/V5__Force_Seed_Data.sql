-- Гарантированное наполнение мастерами, если их меньше 20
DO $$
BEGIN
    IF (SELECT count(*) FROM staff_members WHERE tenant_id = 'tenant-1') < 20 THEN
        FOR i IN 1..25 LOOP
            INSERT INTO staff_members (id, name, specialty, tenant_id, active, phone)
            VALUES ('staff-f-' || i, 'Мастер ' || i, 'Специалист', 'tenant-1', true, '+7900' || LPAD(i::text, 7, '0'))
            ON CONFLICT DO NOTHING;

            INSERT INTO users (id, email, password, role, staff_id, tenant_id)
            VALUES ('user-f-staff-' || i, 'forts' || i || '@e1.ru', '$2a$10$XFMpS9H6xvNPKgSVv.uGxeSJVWJzpy07xd00DMxs.7u41W3uy.G', 'EMPLOYEE', 'staff-f-' || i, 'tenant-1')
            ON CONFLICT DO NOTHING;
        END LOOP;
    END IF;
END $$;

-- Гарантированное наполнение клиентами, если их меньше 50
DO $$
BEGIN
    IF (SELECT count(*) FROM contacts WHERE tenant_id = 'tenant-1') < 50 THEN
        FOR i IN 1..100 LOOP
            INSERT INTO contacts (id, name, phones, email, notes, tenant_id)
            VALUES ('test-f-c-' || i, 'Клиент ' || i, ARRAY['+7' || LPAD((1000000000+i)::text, 10, '0')], 'c' || i || '@test.ru', 'Автозаполнение', 'tenant-1')
            ON CONFLICT DO NOTHING;
        END LOOP;
    END IF;
END $$;
