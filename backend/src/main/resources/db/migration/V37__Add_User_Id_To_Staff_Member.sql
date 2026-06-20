ALTER TABLE staff_members ADD COLUMN user_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL;

UPDATE staff_members sm
SET user_id = u.id
FROM users u
WHERE u.staff_id = sm.id;