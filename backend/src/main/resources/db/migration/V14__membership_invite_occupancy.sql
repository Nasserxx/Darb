ALTER TABLE mosque_join_requests
  ADD COLUMN direction VARCHAR(20) NOT NULL DEFAULT 'MEMBER_REQUEST',
  ADD COLUMN linked_student_id UUID REFERENCES students (id),
  ADD COLUMN relationship VARCHAR(30);

DROP INDEX IF EXISTS mosque_join_requests_one_pending_per_user;

CREATE UNIQUE INDEX mosque_join_requests_one_pending_per_slot
  ON mosque_join_requests (user_id, mosque_id, requested_role)
  WHERE status = 'PENDING';

CREATE UNIQUE INDEX uq_students_user_mosque ON students (user_id, mosque_id);
CREATE UNIQUE INDEX uq_teachers_user_mosque ON teachers (user_id, mosque_id);
