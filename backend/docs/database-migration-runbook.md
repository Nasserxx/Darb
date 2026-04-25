# Database Migration Runbook

## Roles and privileges

- **Migrator role**: used by Flyway at deploy/startup, has DDL permissions (create/alter/index).
- **Runtime role**: used by application at runtime, has DML permissions only (select/insert/update/delete) and no schema-changing privileges.
- Keep role credentials separate to preserve least privilege.

## Required secrets

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `JWT_SECRET`
- Testcontainers tests use ephemeral credentials from the container and should not consume production secrets.

## TLS recommendation

- Require TLS for all non-local PostgreSQL connections.
- Prefer `sslmode=verify-full` and managed CA verification in production.
- Restrict plain-text local-only access to developer environments.

## Rollout checks

1. Run migrations on a staging clone first.
2. Start app with `ddl-auto=validate` and verify boot success.
3. Check Flyway schema history and application health endpoint.
4. Run smoke tests on login, enrollment, attendance, and payment flows.

## Failure strategy

- If a migration fails, stop rollout immediately.
- Fix migration in a new versioned script (never edit an already-applied migration in shared environments).
- Restore from backup or perform forward-fix depending on environment policy.
- Keep SQL review/audit logs for post-incident analysis.
