-- 1. Таблица филиалов
CREATE TABLE IF NOT EXISTS branches (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    timezone VARCHAR(50) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL
);

-- 2. Промежуточная таблица (сначала удаляем, если осталась битая версия)
DROP TABLE IF EXISTS staff_member_branches;
CREATE TABLE staff_member_branches (
    staff_member_id VARCHAR(255) NOT NULL REFERENCES staff_members(id) ON DELETE CASCADE,
    branch_id VARCHAR(255) NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    PRIMARY KEY (staff_member_id, branch_id)
);

-- 3. Безопасное добавление колонки
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='appointments' AND column_name='branch_id') THEN
        ALTER TABLE appointments ADD COLUMN branch_id VARCHAR(255);
    END IF;
END $$;

-- 4. Безопасное добавление внешнего ключа (удаляем старый, если был, и создаем заново)
DO $$
BEGIN
    ALTER TABLE appointments DROP CONSTRAINT IF EXISTS fk_appointment_branch;
    ALTER TABLE appointments
    ADD CONSTRAINT fk_appointment_branch
    FOREIGN KEY (branch_id) REFERENCES branches(id);
END $$;
