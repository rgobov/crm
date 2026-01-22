-- 1. Создание таблицы компаний (tenant_id)
CREATE TABLE companies (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    address VARCHAR(255),
    owner_email VARCHAR(255) NOT NULL
);

-- 2. Создание таблицы сотрудников (мастеров)
CREATE TABLE staff_members (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    specialty VARCHAR(255),
    tenant_id VARCHAR(36) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 3. Создание таблицы пользователей
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    staff_id VARCHAR(36),
    tenant_id VARCHAR(36) NOT NULL,
    telegram_id BIGINT UNIQUE
);

-- 4. Создание таблицы смен
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

-- 5. Создание таблицы контактов (клиентов)
CREATE TABLE contacts (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phones TEXT[] NOT NULL,
    email VARCHAR(255),
    notes TEXT,
    tenant_id VARCHAR(36) NOT NULL
);

-- 6. Создание таблицы услуг
CREATE TABLE services (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    duration_in_minutes INTEGER NOT NULL,
    tenant_id VARCHAR(36) NOT NULL
);

-- 7. Создание таблицы ресурсов (кабинетов)
CREATE TABLE resources (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    tenant_id VARCHAR(36) NOT NULL
);

-- 8. Создание таблицы записей (визитов)
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

-- 9. Создание таблицы комментариев к записям
CREATE TABLE appointment_comments (
    id VARCHAR(36) PRIMARY KEY,
    appointment_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    tenant_id VARCHAR(36) NOT NULL
);

-- 10. Создание таблицы настроек Wappi
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
