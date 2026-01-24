-- 1. СТРУКТУРА ТАБЛИЦ

CREATE TABLE companies (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    address VARCHAR(255),
    owner_email VARCHAR(255) NOT NULL
);

CREATE TABLE staff_members (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    specialty VARCHAR(255),
    tenant_id VARCHAR(36) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    phone VARCHAR(20)
);

CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    staff_id VARCHAR(36),
    tenant_id VARCHAR(36) NOT NULL,
    telegram_id BIGINT UNIQUE
);

CREATE TABLE staff_shifts (
    id VARCHAR(36) PRIMARY KEY,
    staff_id VARCHAR(36) NOT NULL,
    date DATE NOT NULL,
    work_start_time TIME,
    work_end_time TIME,
    break_start_time TIME,
    break_end_time TIME,
    is_day_off BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id VARCHAR(36) NOT NULL
);

CREATE TABLE contacts (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phones TEXT[] NOT NULL,
    email VARCHAR(255),
    notes TEXT,
    tenant_id VARCHAR(36) NOT NULL
);

CREATE TABLE services (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    duration_in_minutes INTEGER NOT NULL,
    tenant_id VARCHAR(36) NOT NULL
);

CREATE TABLE resources (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    tenant_id VARCHAR(36) NOT NULL
);

CREATE TABLE appointments (
    id VARCHAR(36) PRIMARY KEY,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_in_minutes INTEGER NOT NULL,
    client_name VARCHAR(255) NOT NULL,
    contact_id VARCHAR(36),
    service VARCHAR(255) NOT NULL,
    resource_id VARCHAR(36),
    staff_member_id VARCHAR(36),
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    comment TEXT,
    tenant_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    reminder_sent BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE appointment_comments (
    id VARCHAR(36) PRIMARY KEY,
    appointment_id VARCHAR(36) NOT NULL,
    author_id VARCHAR(36) NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    tenant_id VARCHAR(36) NOT NULL
);

CREATE TABLE wappi_settings (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    api_key VARCHAR(255),
    profile_id VARCHAR(255),
    reminder_template TEXT,
    lead_time_minutes INTEGER NOT NULL DEFAULT 1440,
    messenger_type VARCHAR(50) DEFAULT 'TELEGRAM',
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE
);

-- 2. НАПОЛНЕНИЕ ДАННЫМИ

-- Компания
INSERT INTO companies (id, name, address, owner_email)
VALUES ('tenant-1', 'Try Neuro CRM', 'Moscow', 'forts@e1.ru');

-- Администратор (Пароль: qwerty)
INSERT INTO users (id, email, password, role, tenant_id)
VALUES ('admin-id', 'forts@e1.ru', '$2a$10$XFMpS9H6xvNPKgSVv.uGxeSJVWJzpy07xd00DMxs.7u41W3uy.G', 'ADMIN', 'tenant-1');

-- 25 Сотрудников и их пользователи
DO $$
BEGIN
    FOR i IN 1..25 LOOP
        INSERT INTO staff_members (id, name, specialty, tenant_id, active, phone)
        VALUES ('staff-' || i, 'Мастер ' || i, 'Специалист', 'tenant-1', true, '+7900' || LPAD(i::text, 7, '0'));

        INSERT INTO users (id, email, password, role, staff_id, tenant_id)
        VALUES ('user-staff-' || i, 'forts' || i || '@e1.ru', '$2a$10$XFMpS9H6xvNPKgSVv.uGxeSJVWJzpy07xd00DMxs.7u41W3uy.G', 'EMPLOYEE', 'staff-' || i, 'tenant-1');
    END LOOP;
END $$;

-- Роман Гобов
INSERT INTO contacts (id, name, phones, email, notes, tenant_id)
VALUES ('contact-roman', 'Гобов Роман Викторович', ARRAY['79022566116'], 'roman@example.com', 'Главный клиент', 'tenant-1');

-- 100 тестовых клиентов
DO $$
BEGIN
    FOR i IN 1..100 LOOP
        INSERT INTO contacts (id, name, phones, email, notes, tenant_id)
        VALUES ('test-c-' || i, 'Клиент Тестовый ' || i, ARRAY['+7' || LPAD((1000000000+i)::text, 10, '0')], 'c' || i || '@test.ru', 'Автотест', 'tenant-1');
    END LOOP;
END $$;
