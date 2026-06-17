-- Добавление поддержки филиалов для ресурсов
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='resources' AND column_name='branch_id') THEN
        ALTER TABLE resources ADD COLUMN branch_id VARCHAR(255);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name='fk_resources_branch') THEN
        ALTER TABLE resources
        ADD CONSTRAINT fk_resources_branch
        FOREIGN KEY (branch_id) REFERENCES branches(id);
    END IF;
END $$;
