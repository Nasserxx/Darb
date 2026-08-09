# User API — Testing Guide

Step-by-step manual test plan for **`/api/v1/users`** (User Management),
mapped to the project's testing considerations:

1. Press everything
2. Test logic
3. DB changes
4. Flow — normal and logical
5. Role UI
6. Results documentation

Automated matrix: run `.\run-tests.ps1` in this folder (prints results table +
`results.csv`).

## 0. Preconditions

- Backend running (port 8089) with the 5 seed users.
- Create one **throwaway test user** via register (`test.user@darb.app` /
  `Test123!`) — never use seed accounts for update/delete tests.
- Tokens per role: login as SUPER_ADMIN, MOSQUE_ADMIN, TEACHER, STUDENT, PARENT.
- DBeaver open on the `darb` DB.

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/users` | SUPER_ADMIN |
| 2 | GET | `/api/v1/users/search?q=` | SUPER_ADMIN, MOSQUE_ADMIN, TEACHER |
| 3 | GET | `/api/v1/users/{id}` | SUPER_ADMIN, MOSQUE_ADMIN (same mosque), anyone → self |
| 4 | GET | `/api/v1/users/me` | any authenticated |
| 5 | PUT | `/api/v1/users/me` | any authenticated |
| 6 | PUT | `/api/v1/users/{id}` | SUPER_ADMIN |
| 7 | DELETE | `/api/v1/users/{id}` | SUPER_ADMIN |

Hit every endpoint per allowed role, plus no token (401) and a garbage token (401).

## 2. Step-by-step

**Step 1 — Auth basics:** no token → 401 on all 7. Garbage token → 401. Valid
token on `/me` → 200, own data.

**Step 2 — GET `/users` (pagination):** SUPER_ADMIN → 200 with pagination fields
(`totalElements`, `pageNumber`, `last`); test `?page=1&size=2`; out-of-range page
→ empty `content`, 200. MOSQUE_ADMIN/TEACHER/STUDENT/PARENT → 403.

**Step 3 — GET `/users/search?q=`:** SUPER_ADMIN `q=@darb.app` → all 5 seeds
(global). Partial-name search. Missing/blank `q` → 400. MOSQUE_ADMIN & TEACHER
with no mosque assignment → 200 empty page (to test real scoping you need a
mosque + teacher/student profiles; then results must be mosque-only).
STUDENT/PARENT → 403.

**Step 4 — GET `/users/{id}`:** SUPER_ADMIN → any seed user 200. MOSQUE_ADMIN →
self 200, another user 403. STUDENT/TEACHER/PARENT → other 403, self 200
(logic: `callerId == id` bypass). Invalid UUID → 400. Random UUID → 404.

**Step 5 — GET `/users/me`:** all roles → 200, `id` matches JWT subject.

**Step 6 — PUT `/users/me`:** update `fullName` → 200. Update `phone`, `gender`,
`dateOfBirth`, `avatarUrl`. `fullName` > 150 chars → 400 validation. **Logic:**
send `email`/`role`/`password` in body → ignored (unchanged in response).
`createdAt` unchanged, `updatedAt` bumped.

**Step 7 — PUT `/users/{id}` (SUPER_ADMIN):** update throwaway user → 200.
Other roles → 403. Nonexistent id → 404.

**Step 8 — DELETE `/users/{id}` (SUPER_ADMIN):** deactivate throwaway user → 200.
Other roles → 403. Then login as that user → 401 `"Account is deactivated"`.
Row still exists in DB (soft delete).

## 3. DB changes (DBeaver)

- `PUT /users/me` → `full_name` updated, `updated_at` changed, `created_at`
  untouched, `version` bumped.
- `DELETE /users/{id}` → `is_active=false`, row present, `updated_at` bumped.
- **Audit logic:** does SUPER_ADMIN `PUT/DELETE /users/{id}` write
  `override_audit_log`? Per SYSTEM.md SUPER_ADMIN cross-tenant writes require an
  audit reason; `UserService.update/delete` do **not** call `OverrideAuditService`
  — if no audit row appears, record as a gap.

## 4. Flow — normal and logical

- Happy path: register → login → GET `/me` → PUT `/me` → logout → login again.
- Deactivated user: login → 401 → still found via search → re-activate? Note:
  there is **no restore endpoint** (undo deactivation via DB only). Document it.

## 5. Role UI (frontend)

- SUPER_ADMIN: full list / search / edit / deactivate controls.
- MOSQUE_ADMIN/TEACHER: search works; list/delete controls **absent** (illegal
  CTAs absent, not greyed).
- STUDENT/PARENT: no user-management entry points; `/me` editing works.

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. For
manual-only cases use:

| # | Endpoint | Role | Input | Expected | Actual | DB after | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | GET /users | SUPER_ADMIN | — | 200, paginated | | | ✅/❌ |

## Known gaps to flag

- (a) No override-audit on SUPER_ADMIN user writes.
- (b) No restore/deactivate-undo endpoint.
- (c) Mosque-scoped search can't be validated with seed data alone (no
  mosque/profile wiring).
