CREATE TABLE lesson_assignment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id),
    circle_id UUID NOT NULL REFERENCES circles(id),
    half_page_ids JSONB NOT NULL,
    note TEXT,
    assigned_by UUID NOT NULL REFERENCES users(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    UNIQUE (student_id, circle_id)
);

CREATE TABLE memorization_attempt (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id),
    circle_id UUID NOT NULL REFERENCES circles(id),
    page SMALLINT NOT NULL CHECK (page BETWEEN 1 AND 604),
    half CHAR(1) NOT NULL CHECK (half IN ('A', 'B')),
    edition VARCHAR(20) NOT NULL DEFAULT 'MADINAH_604',
    session_date DATE NOT NULL,
    assessor_id UUID NOT NULL REFERENCES users(id),
    grade VARCHAR(15),
    notes TEXT,
    stamps JSONB NOT NULL DEFAULT '[]',
    tajweed_count INT NOT NULL DEFAULT 0,
    hifz_count INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    UNIQUE (student_id, circle_id, page, half)
);

CREATE INDEX idx_mem_attempt_student ON memorization_attempt(student_id);
CREATE INDEX idx_mem_attempt_circle ON memorization_attempt(circle_id);
