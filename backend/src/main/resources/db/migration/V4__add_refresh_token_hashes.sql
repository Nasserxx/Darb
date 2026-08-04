CREATE TABLE refresh_token_hashes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX refresh_token_hashes_token_hash_idx
    ON refresh_token_hashes (token_hash);

CREATE INDEX refresh_token_hashes_user_id_idx
    ON refresh_token_hashes (user_id);
