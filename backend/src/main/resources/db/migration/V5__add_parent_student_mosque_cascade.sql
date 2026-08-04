ALTER TABLE parent_student ADD COLUMN mosque_id UUID REFERENCES mosques(id);
CREATE INDEX idx_parent_student_mosque ON parent_student (mosque_id);
