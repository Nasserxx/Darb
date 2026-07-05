CREATE TABLE mosque_join_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    mosque_id UUID NOT NULL REFERENCES mosques (id),
    requested_role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    reviewed_at TIMESTAMPTZ,
    reviewed_by UUID REFERENCES users (id)
);

CREATE UNIQUE INDEX mosque_join_requests_one_pending_per_user
    ON mosque_join_requests (user_id)
    WHERE status = 'PENDING';

CREATE INDEX mosque_join_requests_mosque_status_idx
    ON mosque_join_requests (mosque_id, status);
