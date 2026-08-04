CREATE TABLE override_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID NOT NULL REFERENCES users (id),
    mosque_id UUID REFERENCES mosques (id),
    action VARCHAR(100) NOT NULL,
    reason TEXT NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX override_audit_log_actor_id_idx ON override_audit_log (actor_id);
CREATE INDEX override_audit_log_mosque_id_idx ON override_audit_log (mosque_id);
CREATE INDEX override_audit_log_resource_idx ON override_audit_log (resource_type, resource_id);
