CREATE TABLE users (
    id uuid PRIMARY KEY,
    full_name varchar(150) NOT NULL,
    email varchar(255) NOT NULL UNIQUE,
    phone varchar(30) UNIQUE,
    password_hash varchar(255) NOT NULL,
    role varchar(30) NOT NULL,
    gender varchar(10),
    date_of_birth date,
    avatar_url text,
    is_active boolean NOT NULL,
    last_login timestamp with time zone,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT chk_users_role CHECK (role IN ('SUPER_ADMIN', 'MOSQUE_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')),
    CONSTRAINT chk_users_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE'))
);

CREATE TABLE mosques (
    id uuid PRIMARY KEY,
    name varchar(200) NOT NULL,
    address text,
    city varchar(100),
    phone varchar(30),
    email varchar(255),
    logo_url text,
    timezone varchar(50),
    settings jsonb,
    is_active boolean NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone
);

CREATE TABLE teachers (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users (id),
    mosque_id uuid NOT NULL REFERENCES mosques (id),
    specialization varchar(150),
    bio text,
    years_experience integer,
    ijazah_chain varchar(500),
    is_available boolean NOT NULL,
    is_active boolean NOT NULL,
    joined_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone
);

CREATE TABLE students (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users (id),
    mosque_id uuid NOT NULL REFERENCES mosques (id),
    national_id varchar(50),
    medical_notes text,
    memorized_juz integer,
    total_absences integer,
    total_late_arrivals integer,
    status varchar(20),
    enrolled_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT chk_students_status CHECK (status IS NULL OR status IN ('PENDING', 'ACTIVE', 'WITHDRAWN', 'COMPLETED', 'REJECTED'))
);

CREATE TABLE mosque_admins (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users (id),
    mosque_id uuid NOT NULL REFERENCES mosques (id),
    permission varchar(30) NOT NULL,
    is_primary_admin boolean NOT NULL,
    assigned_at timestamp with time zone NOT NULL,
    assigned_by uuid NOT NULL REFERENCES users (id),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT chk_mosque_admins_permission CHECK (permission IN ('FULL_ACCESS', 'MANAGE_TEACHERS', 'MANAGE_STUDENTS', 'MANAGE_CIRCLES', 'MANAGE_PAYMENTS', 'VIEW_REPORTS'))
);

CREATE TABLE circles (
    id uuid PRIMARY KEY,
    mosque_id uuid NOT NULL REFERENCES mosques (id),
    teacher_id uuid NOT NULL REFERENCES teachers (id),
    name varchar(200) NOT NULL,
    level varchar(20) NOT NULL,
    type varchar(20) NOT NULL,
    status varchar(20) NOT NULL,
    capacity integer NOT NULL,
    start_time time NOT NULL,
    end_time time NOT NULL,
    days_of_week varchar(100),
    room_or_link text,
    late_threshold_minutes integer,
    monthly_fee numeric(10,2),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT chk_circles_level CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'MEMORIZATION', 'IJAZAH')),
    CONSTRAINT chk_circles_type CHECK (type IN ('IN_PERSON', 'ONLINE', 'HYBRID')),
    CONSTRAINT chk_circles_status CHECK (status IN ('PLANNING', 'ACTIVE', 'PAUSED', 'ENDED'))
);

CREATE TABLE enrollments (
    id uuid PRIMARY KEY,
    student_id uuid NOT NULL REFERENCES students (id),
    circle_id uuid NOT NULL REFERENCES circles (id),
    status varchar(20) NOT NULL,
    enrolled_date date NOT NULL,
    withdrawn_date date,
    approved_by uuid NOT NULL REFERENCES users (id),
    notes text,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT uq_enrollments_student_circle UNIQUE (student_id, circle_id),
    CONSTRAINT chk_enrollments_status CHECK (status IN ('PENDING', 'ACTIVE', 'WITHDRAWN', 'COMPLETED', 'REJECTED'))
);

CREATE TABLE attendance (
    id uuid PRIMARY KEY,
    enrollment_id uuid NOT NULL REFERENCES enrollments (id),
    circle_id uuid NOT NULL REFERENCES circles (id),
    session_date date NOT NULL,
    status varchar(15) NOT NULL,
    scheduled_start time NOT NULL,
    actual_check_in time,
    minutes_late integer,
    parent_notified boolean,
    absence_reason varchar(20),
    excuse_document_url text,
    recorded_by uuid NOT NULL REFERENCES users (id),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT uq_attendance_enrollment_session UNIQUE (enrollment_id, session_date),
    CONSTRAINT chk_attendance_status CHECK (status IN ('PRESENT', 'LATE', 'ABSENT', 'EXCUSED', 'HOLIDAY')),
    CONSTRAINT chk_attendance_absence_reason CHECK (absence_reason IS NULL OR absence_reason IN ('SICK', 'FAMILY', 'TRAVEL', 'PERSONAL', 'OTHER'))
);

CREATE TABLE memorization_progress (
    id uuid PRIMARY KEY,
    student_id uuid NOT NULL REFERENCES students (id),
    circle_id uuid NOT NULL REFERENCES circles (id),
    teacher_id uuid NOT NULL REFERENCES teachers (id),
    surah_number integer NOT NULL,
    ayah_from integer NOT NULL,
    ayah_to integer NOT NULL,
    grade varchar(15) NOT NULL,
    tajweed_score integer,
    teacher_notes text,
    audio_url text,
    session_date date NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT chk_memorization_grade CHECK (grade IN ('EXCELLENT', 'VERY_GOOD', 'GOOD', 'ACCEPTABLE', 'POOR'))
);

CREATE TABLE goals (
    id uuid PRIMARY KEY,
    student_id uuid NOT NULL REFERENCES students (id),
    circle_id uuid NOT NULL REFERENCES circles (id),
    title varchar(200) NOT NULL,
    target_surah integer,
    target_juz integer,
    status varchar(15) NOT NULL,
    due_date date,
    completed_date date,
    set_by uuid NOT NULL REFERENCES users (id),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT chk_goals_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'OVERDUE', 'CANCELLED'))
);

CREATE TABLE parent_student (
    id uuid PRIMARY KEY,
    parent_user_id uuid NOT NULL REFERENCES users (id),
    student_id uuid NOT NULL REFERENCES students (id),
    relationship varchar(30),
    is_primary boolean NOT NULL,
    receives_notifications boolean NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT uq_parent_student_pair UNIQUE (parent_user_id, student_id)
);

CREATE TABLE payments (
    id uuid PRIMARY KEY,
    student_id uuid NOT NULL REFERENCES students (id),
    circle_id uuid NOT NULL REFERENCES circles (id),
    mosque_id uuid NOT NULL REFERENCES mosques (id),
    amount numeric(10,2) NOT NULL,
    discount numeric(10,2),
    amount_paid numeric(10,2),
    status varchar(15) NOT NULL,
    method varchar(20) NOT NULL,
    cycle varchar(15) NOT NULL,
    due_date date,
    paid_date date,
    receipt_url text,
    recorded_by uuid NOT NULL REFERENCES users (id),
    notes text,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'PARTIAL', 'PAID', 'OVERDUE', 'WAIVED', 'REFUNDED')),
    CONSTRAINT chk_payments_method CHECK (method IN ('CASH', 'BANK_TRANSFER', 'CARD', 'ONLINE', 'OTHER')),
    CONSTRAINT chk_payments_cycle CHECK (cycle IN ('MONTHLY', 'QUARTERLY', 'SEMESTER', 'ANNUAL', 'ONE_TIME'))
);

CREATE TABLE notifications (
    id uuid PRIMARY KEY,
    mosque_id uuid NOT NULL REFERENCES mosques (id),
    recipient_user_id uuid NOT NULL REFERENCES users (id),
    sender_user_id uuid NOT NULL REFERENCES users (id),
    title varchar(200) NOT NULL,
    body text NOT NULL,
    channel varchar(15) NOT NULL,
    status varchar(15) NOT NULL,
    sent_at timestamp with time zone,
    delivered_at timestamp with time zone,
    metadata jsonb,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT chk_notifications_channel CHECK (channel IN ('IN_APP', 'SMS', 'EMAIL', 'PUSH')),
    CONSTRAINT chk_notifications_status CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'READ'))
);

CREATE TABLE messages (
    id uuid PRIMARY KEY,
    sender_id uuid NOT NULL REFERENCES users (id),
    receiver_id uuid NOT NULL REFERENCES users (id),
    circle_id uuid NOT NULL REFERENCES circles (id),
    content text NOT NULL,
    status varchar(15) NOT NULL,
    sent_at timestamp with time zone NOT NULL,
    read_at timestamp with time zone,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT chk_messages_status CHECK (status IN ('SENT', 'DELIVERED', 'READ'))
);

CREATE TABLE achievements (
    id uuid PRIMARY KEY,
    student_id uuid NOT NULL REFERENCES students (id),
    mosque_id uuid NOT NULL REFERENCES mosques (id),
    type varchar(20) NOT NULL,
    title varchar(200) NOT NULL,
    description text,
    badge_url text,
    awarded_by uuid NOT NULL REFERENCES users (id),
    awarded_date date,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT chk_achievements_type CHECK (type IN ('MEMORIZATION', 'ATTENDANCE', 'RECITATION', 'COMPETITION', 'MILESTONE'))
);

CREATE TABLE reports (
    id uuid PRIMARY KEY,
    mosque_id uuid NOT NULL REFERENCES mosques (id),
    generated_by uuid NOT NULL REFERENCES users (id),
    type varchar(30) NOT NULL,
    title varchar(200) NOT NULL,
    filters jsonb,
    file_url text,
    generated_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT chk_reports_type CHECK (type IN ('ATTENDANCE_SUMMARY', 'FINANCIAL', 'STUDENT_PROGRESS', 'TEACHER_PERFORMANCE', 'ENROLLMENT'))
);

CREATE INDEX idx_teachers_user_id ON teachers (user_id);
CREATE INDEX idx_teachers_mosque_id ON teachers (mosque_id);
CREATE INDEX idx_students_user_id ON students (user_id);
CREATE INDEX idx_students_mosque_id ON students (mosque_id);
CREATE INDEX idx_students_status ON students (status);
CREATE INDEX idx_mosque_admins_user_id ON mosque_admins (user_id);
CREATE INDEX idx_mosque_admins_mosque_id ON mosque_admins (mosque_id);
CREATE INDEX idx_mosque_admins_assigned_by ON mosque_admins (assigned_by);
CREATE INDEX idx_circles_mosque_id ON circles (mosque_id);
CREATE INDEX idx_circles_teacher_id ON circles (teacher_id);
CREATE INDEX idx_enrollments_student_id ON enrollments (student_id);
CREATE INDEX idx_enrollments_circle_id ON enrollments (circle_id);
CREATE INDEX idx_enrollments_status ON enrollments (status);
CREATE INDEX idx_enrollments_approved_by ON enrollments (approved_by);
CREATE INDEX idx_attendance_enrollment_id ON attendance (enrollment_id);
CREATE INDEX idx_attendance_circle_id ON attendance (circle_id);
CREATE INDEX idx_attendance_recorded_by ON attendance (recorded_by);
CREATE INDEX idx_attendance_session_status ON attendance (session_date, status);
CREATE INDEX idx_memorization_student_id ON memorization_progress (student_id);
CREATE INDEX idx_memorization_circle_id ON memorization_progress (circle_id);
CREATE INDEX idx_memorization_teacher_id ON memorization_progress (teacher_id);
CREATE INDEX idx_goals_student_id ON goals (student_id);
CREATE INDEX idx_goals_circle_id ON goals (circle_id);
CREATE INDEX idx_goals_set_by ON goals (set_by);
CREATE INDEX idx_parent_student_parent ON parent_student (parent_user_id);
CREATE INDEX idx_parent_student_student ON parent_student (student_id);
CREATE INDEX idx_payments_student_id ON payments (student_id);
CREATE INDEX idx_payments_circle_id ON payments (circle_id);
CREATE INDEX idx_payments_mosque_id ON payments (mosque_id);
CREATE INDEX idx_payments_recorded_by ON payments (recorded_by);
CREATE INDEX idx_payments_status_due_date ON payments (status, due_date);
CREATE INDEX idx_notifications_mosque_id ON notifications (mosque_id);
CREATE INDEX idx_notifications_recipient_status ON notifications (recipient_user_id, status);
CREATE INDEX idx_notifications_sender_id ON notifications (sender_user_id);
CREATE INDEX idx_messages_sender_id ON messages (sender_id);
CREATE INDEX idx_messages_receiver_status ON messages (receiver_id, status);
CREATE INDEX idx_messages_circle_id ON messages (circle_id);
CREATE INDEX idx_achievements_student_id ON achievements (student_id);
CREATE INDEX idx_achievements_mosque_id ON achievements (mosque_id);
CREATE INDEX idx_achievements_awarded_by ON achievements (awarded_by);
CREATE INDEX idx_reports_mosque_id ON reports (mosque_id);
CREATE INDEX idx_reports_generated_by ON reports (generated_by);
CREATE INDEX idx_users_role ON users (role);
