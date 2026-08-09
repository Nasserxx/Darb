# Message API — Testing Guide

Step-by-step manual test plan for **`/api/v1/messages`** (direct and
circle-threaded messages), mapped to the project's testing considerations:

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
- At least one **circle** in the DB — POST and `GET /circle/{id}` resolve
  `circleId` against `circles`, so without one the success cases are `SKIPPED`
  and only 404/400 paths are exercised. Create one via the circle-api workspace
  (`POST /api/v1/circles` as SUPER_ADMIN or MOSQUE_ADMIN).
- Tokens per role: login as SUPER_ADMIN, MOSQUE_ADMIN, TEACHER, STUDENT, PARENT.
- DBeaver open on the `darb` DB.

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/messages/mine` | any authenticated |
| 2 | GET | `/api/v1/messages/circle/{circleId}` | any authenticated (participant or mosque access) |
| 3 | POST | `/api/v1/messages` | any authenticated |
| 4 | PUT | `/api/v1/messages/{id}/read` | any authenticated |

Hit every endpoint per allowed role, plus no token (401) and a garbage token (401).

## 2. Step-by-step

**Step 1 — Auth basics:** no token → 401 on `/mine`, `/circle/{id}`, POST, and
`/{id}/read`. Garbage token → 401.

**Step 2 — GET `/mine` (all roles):** SUPER_ADMIN, MOSQUE_ADMIN, TEACHER,
STUDENT, PARENT → 200 with pagination fields. Shows messages where the caller
is sender or receiver.

**Step 3 — GET `/circle/{circleId}`:**
- SUPER_ADMIN on a real circle → 200 (bypasses mosque check).
- Non-participant with no mosque assignment (e.g., seed STUDENT without a
  profile) → 403. Participants → 200.
- Nonexistent circle → 404. Invalid UUID → 400. No token → 401.

**Step 4 — POST `/messages` (any authenticated):**
- Valid body (`receiverId`, `circleId`, `content`) → 201, `senderId` = caller
  (body value ignored), `status=SENT`.
- Same request as STUDENT, TEACHER, PARENT → 201 (any role).
- Missing required fields (`{}`) → 400. Blank `content` → 400.
- Nonexistent `circleId` → 404. Nonexistent `receiverId` → 404.

**Step 5 — PUT `/{id}/read` (any authenticated):** mark an existing message →
200 (`status=READ`, `readAt` set). Nonexistent id → 404. Invalid UUID → 400.
No token → 401. **Logic:** no ownership check in `MessageService.markAsRead` —
any authenticated user can mark any message READ, even one they neither sent
nor received. Verify and record.

## 3. DB changes (DBeaver)

- `POST /messages` → new row in `messages`: `sender_id`, `receiver_id`,
  `circle_id`, `content`, `status='SENT'`, `sent_at` (now), `created_at`,
  `version=0`.
- `PUT /{id}/read` → `status='READ'`, `read_at` set, `updated_at` bumped.
- `GET /circle/{id}` as a non-participant exercises the
  `assertCanAccessMosque` branch — 403 when the caller has no mosque.

## 4. Flow — normal and logical

- Happy path: user A sends to user B in a circle → both see it in `GET /mine`
  with the same `id` and `circleId` → B marks it read → `status=READ`,
  `readAt` set → B's `GET /mine` reflects it.
- Thread view: `GET /circle/{circleId}` returns only that circle's messages.
- Non-participant: user C (no mosque) hits `GET /circle/{id}` → 403; with a
  mosque assignment but not in the circle → mosque-scope determines access.

## 5. Role UI (frontend)

- All roles: `/:locale/messages` (inbox table) and `/:locale/messages/circle/:id`
  (thread view with send form). Mark-read via row action.
- Sender/receiver pickers drive `receiverId`; circle threads are entered from
  the inbox — verify no CTA exposes arbitrary circle ids to non-participants.

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. For
manual-only cases use:

| # | Endpoint | Role | Input | Expected | Actual | DB after | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | POST /messages | STUDENT | circle+receiver ids | 201, row inserted | | | ✅/❌ |

## Known gaps to flag

- (a) `PUT /{id}/read` enforces **no sender/receiver ownership check** — any
  authenticated user can mark any message READ.
- (b) `POST /messages` performs **no circle participant/mosque assertion** — any
  authenticated user can post into any circle thread (service only checks that
  the circle exists).
- (c) `MessageUpdateRequest` exists but there is **no `PUT /messages/{id}`
  endpoint** — status can only be changed via the dedicated `/read` action.
