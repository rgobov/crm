-- 1. Создаем компанию
INSERT INTO companies (id, name, address, owner_email)
VALUES ('tenant-1', 'Try Neuro CRM', 'Moscow', 'forts@e1.ru')
ON CONFLICT DO NOTHING;

-- 2. Создаем администратора (Email: forts@e1.ru, Пароль: qwerty)
-- BCrypt hash для 'qwerty'
INSERT INTO users (id, email, password, role, tenant_id)
VALUES ('admin-id', 'forts@e1.ru', '$2a$10$XFMpS9H6xvNPKgSVv.uGxeSJVWJzpy07xd00DMxs.7u41W3u', 'ADMIN', 'tenant-1')
ON CONFLICT (email) DO NOTHING;

-- 3. Создаем 25 сотрудников и их учетные записи
DO $$
BEGIN
    FOR i IN 1..25 LOOP
        -- Сотрудник
        INSERT INTO staff_members (id, name, specialty, tenant_id, active, phone)
        VALUES (
            'staff-id-' || i,
            'Мастер ' || i,
            CASE WHEN i % 2 = 0 THEN 'Топ-мастер' ELSE 'Стилист' END,
            'tenant-1',
            true,
            '+7900' || LPAD(i::text, 7, '0')
        );

        -- Пользователь для сотрудника (Email: fortsX@e1.ru, Пароль: qwerty)
        INSERT INTO users (id, email, password, role, staff_id, tenant_id)
        VALUES (
            'user-staff-id-' || i,
            'forts' || i || '@e1.ru',
            '$2a$10$XFMpS9H6xvNPKgSVv.uGxeSJVWJzpy07xd00DMxs.7u41W3u',
            'EMPLOYEE',
            'staff-id-' || i,
            'tenant-1'
        );
    END LOOP;
END $$;

-- 4. Создаем Гобова Романа Викторовича
INSERT INTO contacts (id, name, phones, email, notes, tenant_id)
VALUES ('contact-roman', 'Гобов Роман Викторович', ARRAY['79022566116'], 'roman@example.com', 'Главный клиент', 'tenant-1')
ON CONFLICT DO NOTHING;

-- 5. Создаем еще 100 тестовых клиентов
DO $$
BEGIN
    FOR i IN 1..100 LOOP
        INSERT INTO contacts (id, name, phones, email, notes, tenant_id)
        VALUES (
            'test-contact-' || i,
            'Клиент Тестовый ' || i,
            ARRAY['+7' || LPAD((1000000000 + i)::text, 10, '0')],
            'client' || i || '@test.ru',
            'Автотест',
            'tenant-1'
        );
    END LOOP;
END $$;
