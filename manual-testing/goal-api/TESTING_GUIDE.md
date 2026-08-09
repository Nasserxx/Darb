# Goal API — Testing Guide

Step-by-step manual test plan for **`/api/v1/goals`** (Goal Management: setting
and tracking Quran memorization goals for students), mapped to the project's
testing considerations:

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
- **Domain data required for successful writes**: goals reference a **Student**
  (`studentId`), a **Circle** (`circleId`) and the **caller as setBy**. No
  enrollment or mosque wiring is required to create a goal. The seed users have
  no student/circle profiles, so on a fresh DB creates are `SKIPPED` by the
  script.
- Tokens per role: login as SUPER_ADMIN, MOSQUE_ADMIN, TEACHER, STUDENT, PARENT.
- DBeaver open on the `darb` DB (`goals` table).

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/goals/{id}` | any authenticated (student access enforced) |
| 2 | GET | `/api/v1/goals/student/{studentId}` | any authenticated (NO access assertion) |
| 3 | POST | `/api/v1/goals` | SUPER_ADMIN, MOSQUE_ADMIN, TEACHER |
| 4 | PUT | `/api/v1/goals/{id}` | SUPER_ADMIN, MOSQUE_ADMIN, TEACHER |

Enums:
- `GoalStatus`: `IN_PROGRESS`, `COMPLETED`, `OVERDUE`, `CANCELLED`.

Hit every endpoint per allowed role, plus no token (401), garbage token (401),
invalid enum (400), and nonexistent id (404).

## 2. Step-by-step

**Step 1 — Auth basics:** no token → 401 on all 4. Garbage token → 401.

**Step 2 — POST `/goals` (create):** SUPER_ADMIN / MOSQUE_ADMIN / TEACHER with a
real student + circle → 201. **Note:** `GoalService.create` performs **no
tenant/mosque assertion**, so MOSQUE_ADMIN and TEACHER succeed even without a
mosque assignment (gap, flagged below). STUDENT/PARENT → 403. Empty body → 400;
`title` > 200 chars → 400; `status="DONE"` → 400. Nonexistent student → 404;
nonexistent circle → 404. No enrollment is required.

**Step 3 — GET `/goals/{id}`:** existing goal → SUPER_ADMIN 200, PARENT without
link → 403. Random UUID → 404. Invalid UUID → 400.

**Step 4 — GET `/goals/student/{studentId}`:** **No access assertion** — any
authenticated user (e.g., an unlinked PARENT) can list a student's goals → 200.
Random UUID → **200 empty page** (no student existence check). Invalid UUID →
400.

**Step 5 — PUT `/goals/{id}`:** existing goal → SUPER_ADMIN 200. **No access
assertion** — TEACHER can update any goal → 200 (gap). STUDENT → 403. Invalid
`status` enum → 400. Nonexistent id → 404.

## 3. DB changes (DBeaver)

- `POST /goals` → row in `goals` (`student_id`, `circle_id`, `title`,
  `target_surah`, `target_juz`, `status`, `due_date`, `set_by`).
- `PUT /goals/{id}` → `title` / `target_surah` / `target_juz` / `status` /
  `due_date` / `completed_date` updated.
- There is **no list-all** and **no DELETE** endpoint (controller has neither;
  `GoalService.findAll`/`delete` are not exposed).

## 4. Flow — normal and logical

- Happy path: TEACHER sets a goal (title, target surah/juz, due date) → student
  works toward it → TEACHER marks `COMPLETED` + `completedDate` → student/parent
  see the status via `GET /goals/student/{studentId}`.
- Logic checks worth re-testing by hand: goals are **not** tied to enrollment
  (a goal can exist for a student who left the circle); `OVERDUE` is a stored
  status, not auto-computed.

## 5. Role UI (frontend)

- TEACHER/MOSQUE_ADMIN/SUPER_ADMIN: create/edit goals for students.
- STUDENT: view own goals (student-scoped).
- PARENT: view child goals.
- **Risk:** because `GET /goals/student/{studentId}` has no access check, any
  authenticated role can enumerate another student's goals — no UI entry point
  exposes this, but the API allows it.

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. For
manual-only cases use:

| # | Endpoint | Role | Input | Expected | Actual | DB after | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | POST /goals | TEACHER | student + circle | 201 | | | ✅/❌ |
| 2 | PUT /goals/{id} | TEACHER | status=COMPLETED | 200 | | | ✅/❌ |

## Known gaps to flag

- (a) `status` is `NOT NULL` in the DB but optional in `GoalCreateRequest` —
  omitting it causes a DB error (500), not a 400.
- (b) `GET /goals/student/{studentId}` has **no access assertion** — any
  authenticated user can list any student's goals (and a random UUID returns
  200 empty, not 404).
- (c) `POST /goals` and `PUT /goals/{id}` perform **no tenant assertion** —
  any MOSQUE_ADMIN/TEACHER can create/update a goal for any student, with no
  mosque relationship required.
- (d) No list-all and no DELETE mapping on the controller, though the service
  implements both.
