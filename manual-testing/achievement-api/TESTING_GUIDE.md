# Achievement API — Testing Guide

Step-by-step manual test plan for **`/api/v1/achievements`** (student
achievements and badges), mapped to the project's testing considerations:

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
- At least one **student profile** and one **mosque** in the DB — POST resolves
  `studentId`/`mosqueId` and `GET /{id}` asserts student access, so without
  them the success cases are `SKIPPED` and only 404/400/403 paths run. Create a
  student via the student-api workspace (`POST /api/v1/students`) and a mosque
  via the mosque-api workspace.
- Tokens per role: login as SUPER_ADMIN, MOSQUE_ADMIN, TEACHER, STUDENT, PARENT.
- DBeaver open on the `darb` DB.

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/achievements/{id}` | any authenticated (service asserts student access) |
| 2 | GET | `/api/v1/achievements/student/{studentId}` | any authenticated |
| 3 | GET | `/api/v1/achievements/mosque/{mosqueId}` | SUPER_ADMIN, MOSQUE_ADMIN |
| 4 | POST | `/api/v1/achievements` | SUPER_ADMIN, MOSQUE_ADMIN, TEACHER |

Hit every endpoint per allowed role, plus no token (401) and a garbage token (401).

## 2. Step-by-step

**Step 1 — Auth basics:** no token → 401 on all four. Garbage token → 401.

**Step 2 — GET `/{id}` (any authenticated):**
- SUPER_ADMIN → 200 (bypasses student check).
- STUDENT → 200 only for their own achievement; otherwise 403.
- MOSQUE_ADMIN / TEACHER → 200 for students in their mosque, else 403 (no
  mosque assignment on the seeds → 403).
- PARENT → 200 only for a linked child, else 403 (no link → 403).
- Nonexistent id → 404. Invalid UUID → 400.

**Step 3 — GET `/student/{studentId}` (any authenticated):**
- All roles → 200 (paginated). **Note:** the service does **not** call
  `assertCanAccessStudent` here — any authenticated user can list any student's
  achievements.
- Nonexistent student id → 200 with an empty page (no existence check; the API
  docs claim 404 — verify and record). Invalid UUID → 400.

**Step 4 — GET `/mosque/{mosqueId}` (SUPER_ADMIN, MOSQUE_ADMIN):**
- SUPER_ADMIN and MOSQUE_ADMIN → 200. TEACHER/STUDENT/PARENT → 403
  (`@PreAuthorize`).
- Nonexistent mosque id → 200 with an empty page (no existence check; docs
  claim 404 — verify and record). Invalid UUID → 400.

**Step 5 — POST `/achievements` (SUPER_ADMIN, MOSQUE_ADMIN, TEACHER):**
- Valid body (`studentId`, `mosqueId`, `type=MEMORIZATION`, `title`) → 201,
  `awardedBy` = caller (body value ignored), `awardedDate` optional.
- All three allowed roles → 201. STUDENT/PARENT → 403.
- Missing required fields (`{}`) → 400. Unknown type (e.g. `type=HACKATHON`) →
  400. `title` > 200 chars → 400.
- Nonexistent `studentId` → 404. Nonexistent `mosqueId` → 404.

## 3. DB changes (DBeaver)

- `POST /achievements` → new row in `achievements`: `student_id`, `mosque_id`,
  `type`, `title`, `description`, `badge_url`, `awarded_by`, `awarded_date`,
  `created_at`, `version=0`.
- `GET /{id}` for MOSQUE_ADMIN/TEACHER/PARENT exercises the
  `assertCanAccessStudent` branches (403 paths above).

## 4. Flow — normal and logical

- Happy path: TEACHER/MOSQUE_ADMIN/SUPER_ADMIN awards → student logs in →
  `GET /student/{selfId}` and `GET /{id}` show it → admin sees it via
  `GET /mosque/{mosqueId}`.
- Isolation: a PARENT without a link and a MOSQUE_ADMIN from another mosque get
  403 on `GET /{id}`; but `GET /student/{studentId}` leaks the same data (no
  check) — compare both on the same achievement.

## 5. Role UI (frontend)

- `/:locale/achievements` — students see their own list; TEACHER/MOSQUE_ADMIN/
  SUPER_ADMIN get a create form (student picker, mosque picker, type, title,
  date). Verify the UI only offers students the caller may award (server does
  not enforce this on POST).

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. For
manual-only cases use:

| # | Endpoint | Role | Input | Expected | Actual | DB after | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | POST /achievements | TEACHER | student+mosque ids | 201, row inserted | | | ✅/❌ |

## Known gaps to flag

- (a) `GET /achievements/student/{studentId}` performs **no student-access
  check** — any authenticated user can list any student's achievements.
- (b) `GET /achievements/mosque/{mosqueId}` and `/student/{studentId}` have
  **no existence check** — a nonexistent id returns an empty 200 page while the
  API docs claim 404.
- (c) `POST /achievements` performs **no mosque-access assertion** — a TEACHER
  can award achievements referencing any student/mosque (service only verifies
  existence).
- (d) `AchievementService.update`/`delete` exist but have **no controller
  endpoint** — awarded achievements can't be edited or removed via API.
