-- Создаем таблицу для связи Many-to-Many между сотрудниками и филиалами
-- Это позволит одному сотруднику (мастеру) работать в нескольких точках одновременно
CREATE TABLE IF NOT EXISTS staff_member_branches (
    staff_member_id VARCHAR(255) NOT NULL REFERENCES staff_members(id) ON DELETE CASCADE,
    branch_id VARCHAR(255) NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    PRIMARY KEY (staff_member_id, branch_id)
);

-- Индексы для быстрого поиска
CREATE INDEX idx_staff_member_branches_staff ON staff_member_branches(staff_member_id);
CREATE INDEX idx_staff_member_branches_branch ON staff_member_branches(branch_id);
