# Notification API — Testing Guide

Step-by-step manual test plan for **`/api/v1/notifications`** (in-app
notifications), mapped to the project's testing considerations:

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
- At least one **mosque** in the DB — the create endpoint resolves `mosqueId`
  against `mosques`, so without one the success cases are `SKIPPED` and only
  404/400 paths are exercised. A mosque can be created via the mosque-api
  workspace (`POST /api/v1/mosques` as SUPER_ADMIN or mosque onboard).
- Tokens per role: login as SUPER_ADMIN, MOSQUE_ADMIN, TEACHER, STUDENT, PARENT.
- DBeaver open on the `darb` DB.

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/notifications/mine` | any authenticated |
| 2 | PUT | `/api/v1/notifications/{id}/read` | any authenticated |
| 3 | POST | `/api/v1/notifications` | SUPER_ADMIN, MOSQUE_ADMIN |

Hit every endpoint per allowed role, plus no token (401) and a garbage token (401).

## 2. Step-by-step

**Step 1 — Auth basics:** no token → 401 on `/mine`, `/read`, and POST. Garbage
token → 401. Any valid token on `/mine` → 200.

**Step 2 — GET `/mine` (all roles):** SUPER_ADMIN, MOSQUE_ADMIN, TEACHER,
STUDENT, PARENT → 200 with pagination fields (`totalElements`, `pageNumber`,
`last`). Empty until notifications exist.

**Step 3 — POST `/notifications` (SUPER_ADMIN, MOSQUE_ADMIN):**
- Valid body (`mosqueId`, `recipientUserId`, `title`, `body`, `channel=IN_APP`,
  `status=PENDING`) → 201, `senderUserId` = caller (body value ignored).
- Allowed roles SUPER_ADMIN and MOSQUE_ADMIN → 201. TEACHER/STUDENT/PARENT → 403.
- Missing required fields (`{}`) → 400. Unknown channel enum (e.g.
  `channel=WHATSAPP`) → 400. `title` > 255 chars → 400.
- Nonexistent `mosqueId` → 404. Nonexistent `recipientUserId` → 404.

**Step 4 — PUT `/{id}/read` (any authenticated):** mark an existing
notification → 200 (`status=READ`, `deliveredAt` set). Nonexistent id → 404.
Invalid UUID → 400. No token → 401. **Logic:** there is no ownership check in
`NotificationService.markAsRead` — any authenticated user can mark *any*
notification READ, even one addressed to someone else. Verify and record.

## 3. DB changes (DBeaver)

- `POST /notifications` → new row in `notifications`: `mosque_id`,
  `recipient_user_id`, `sender_user_id`, `title`, `body`, `channel`, `status`,
  `metadata`, `created_at`, `version=0`. **`sent_at` stays NULL** — the service
  never populates it.
- `PUT /{id}/read` → `status='READ'`, `delivered_at` set, `updated_at` bumped.
- Note: `status` is optional in the request DTO but the column is `NOT NULL`
  with no default — omitting it from the body can error on insert instead of
  defaulting to `PENDING`. Verify.

## 4. Flow — normal and logical

- Happy path: SUPER_ADMIN/MOSQUE_ADMIN sends → recipient logs in → `GET /mine`
  shows it → `PUT /{id}/read` → status `READ` → `GET /mine` still shows it with
  `deliveredAt`.
- Cross-user: mark a notification addressed to another user as read — currently
  returns 200 (no recipient check). If this is undesired, that is a gap.

## 5. Role UI (frontend)

- All roles: `/:locale/notifications` page (inbox table, mark-read button).
- SUPER_ADMIN/MOSQUE_ADMIN: compose/send entry points only if the frontend adds
  them — the API supports POST; verify the UI surfaces it.
- TEACHER/STUDENT/PARENT: receive-only; no send CTA (illegal CTA absent).

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. For
manual-only cases use:

| # | Endpoint | Role | Input | Expected | Actual | DB after | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | POST /notifications | SUPER_ADMIN | mosque+student ids | 201, row inserted | | | ✅/❌ |

## Known gaps to flag

- (a) `PUT /{id}/read` enforces **no recipient/ownership check** — any
  authenticated user can mark any notification READ.
- (b) `POST /notifications` does **no mosque-access assertion** in the service —
  a MOSQUE_ADMIN can target any mosque/user, not just their own.
- (c) `status` is optional in `NotificationCreateRequest` but the column is
  `NOT NULL` without default — omitting it likely errors instead of defaulting
  to `PENDING`.
- (d) `sent_at` is never set by the service — always NULL in DB and response.
- (e) `NotificationService.update`/`delete` exist but have **no controller
  endpoint** — no way to update metadata or delete a notification via API.
