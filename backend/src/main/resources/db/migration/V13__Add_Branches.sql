-- 1. Таблица для хранения филиалов
CREATE TABLE branches (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    timezone VARCHAR(50) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL
);

-- 2. Промежуточная таблица для связи ManyToMany между сотрудниками и филиалами
CREATE TABLE staff_member_branches (
    staff_member_id VARCHAR(255) NOT NULL REFERENCES staff_members(id) ON DELETE CASCADE,
    branch_id VARCHAR(255) NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    PRIMARY KEY (staff_member_id, branch_id)
);

-- 3. Добавляем колонку branch_id в таблицу appointments
ALTER TABLE appointments
ADD COLUMN branch_id VARCHAR(255);

-- 4. Добавляем внешний ключ
ALTER TABLE appointments
ADD CONSTRAINT fk_appointment_branch
FOREIGN KEY (branch_id) REFERENCES branches(id);
