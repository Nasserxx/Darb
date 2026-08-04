ALTER TABLE students ADD COLUMN parent_invite_code VARCHAR(50) UNIQUE;
CREATE INDEX idx_students_parent_invite_code ON students (parent_invite_code) WHERE parent_invite_code IS NOT NULL;
