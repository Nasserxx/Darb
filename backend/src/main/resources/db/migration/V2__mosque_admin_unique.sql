ALTER TABLE mosque_admins
    ADD CONSTRAINT uq_mosque_admins_user_mosque UNIQUE (user_id, mosque_id);
