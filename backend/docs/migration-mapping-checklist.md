# Migration Mapping Checklist

## Scope

This checklist aligns the initial Flyway schema with the current JPA domain model in `com.darb.domain` and the ER source in `backend/resources/er-diagramm.mermaid`.

## Mapping Decisions

- Use PostgreSQL `uuid` PKs for every table, matching `BaseAuditableEntity`.
- Keep audit columns (`created_at`, `updated_at`) on every table from `BaseAuditableEntity`.
- Store enums as `varchar` values (Hibernate `EnumType.STRING`) with explicit `CHECK` constraints.
- Keep JSON fields as `jsonb` where defined (`mosques.settings`, `notifications.metadata`, `reports.filters`).
- Preserve nullable vs non-nullable constraints according to entity annotations (`nullable = false` and optional relations).
- Model monetary fields as `numeric(10,2)` for `Circle.monthlyFee` and `Payment` amounts.

## Relationship Assumptions

- All FK fields in entities are implemented as explicit FK constraints.
- No cascade deletes are enforced in SQL by default; application-level behavior remains explicit.
- `users.email` is unique and required; `users.phone` is unique but nullable.
- `enrollments` has a uniqueness guard on (`student_id`, `circle_id`) to avoid duplicate active links.
- `attendance` has a uniqueness guard on (`enrollment_id`, `session_date`) to avoid duplicate daily records.
- `parent_student` has a uniqueness guard on (`parent_user_id`, `student_id`) to avoid duplicate parent links.

## Indexing Baseline

- Add indexes for all FK columns.
- Add practical filter indexes for high-frequency query fields:
  - `users(role)`
  - `students(status)`
  - `enrollments(status)`
  - `attendance(session_date, status)`
  - `payments(status, due_date)`
  - `notifications(recipient_user_id, status)`
  - `messages(receiver_id, status)`

