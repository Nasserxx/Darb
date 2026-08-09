# Circle API — Testing Guide

Step-by-step manual test plan for **`/api/v1/circles`** (study circles /
halaqat, scheduling, capacity), mapped to the project's testing considerations:

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
- An existing **mosque + teacher** are required for the create/update/delete
  success paths (a circle needs both). The script resolves them dynamically from
  `GET /mosques` and `GET /teachers` (SUPER_ADMIN); on a clean seed DB those
  cases are SKIPPED.
- The script creates a **throwaway circle** for destructive update/delete tests
  and soft-deletes it at the end (status flips to `ENDED`), so seed data is
  never destroyed.
- Tokens per role: login as SUPER_ADMIN, MOSQUE_ADMIN, TEACHER, STUDENT, PARENT.
- DBeaver open on the `darb` DB.

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/circles` | any authenticated |
| 2 | GET | `/api/v1/circles/{id}` | any authenticated (mosque-scoped) |
| 3 | POST | `/api/v1/circles` | SUPER_ADMIN, MOSQUE_ADMIN |
| 4 | PUT | `/api/v1/circles/{id}` | SUPER_ADMIN, MOSQUE_ADMIN |
| 5 | DELETE | `/api/v1/circles/{id}` | SUPER_ADMIN, MOSQUE_ADMIN |

Hit every endpoint per allowed role, plus no token (401) and a garbage token (401).

## 2. Step-by-step

**Step 1 — Auth basics:** no token → 401 on list/create. Garbage token → 401.

**Step 2 — GET `/circles`:** SUPER_ADMIN → 200 (all, global). MOSQUE_ADMIN → 200
(mosque-scoped; empty without an assignment). TEACHER → 200 own circles, but
**404 without a teacher profile** (see Known gaps (a)). STUDENT/PARENT → 200
(empty page — no mosque context via `pageForCaller`).

**Step 3 — GET `/circles/{id}`:** invalid UUID → 400. Random UUID → 404. Existing
circle → SUPER_ADMIN 200; MOSQUE_ADMIN/TEACHER/STUDENT/PARENT → 403 (no mosque
access via `assertCanAccessMosque`).

**Step 4 — POST `/circles`:** empty body → 400 (`mosqueId`, `teacherId`, `name`,
`level`, `type` required). Unknown mosque/teacher → 404. MOSQUE_ADMIN without an
assignment → 403 when the mosque exists. `name` > 200 chars → 400. TEACHER/
STUDENT/PARENT → 403. Success → 201 (SUPER_ADMIN needs **no audit reason** here —
see Known gaps (b)).

**Step 5 — PUT `/circles/{id}`:** `name` > 200 chars → 400; invalid UUID → 400;
random UUID → 404. Update `name`, `status`, `capacity`, `startTime`/`endTime`,
`daysOfWeek`, `roomOrLink`, `lateThresholdMinutes`, `monthlyFee` → 200.
MOSQUE_ADMIN → 200 within mosque, else 403. TEACHER/STUDENT/PARENT → 403.

**Step 6 — DELETE `/circles/{id}`:** random UUID → 404; invalid UUID → 400.
Success → 200 = **soft delete** (`status` flips to `ENDED`, row stays).
MOSQUE_ADMIN → 200 within mosque, else 403. TEACHER/STUDENT/PARENT → 403.

## 3. DB changes (DBeaver)

- `circles` rows: `mosque_id`, `teacher_id`, `name`, `level`, `type`, `status`,
  `capacity`, `start_time`, `end_time`, `days_of_week`, `room_or_link`,
  `late_threshold_minutes`, `monthly_fee`, `created_at`.
- DELETE → `status='ENDED'`; the row remains and `updated_at` bumps.
- **Audit logic:** no `override_audit_log` rows for circle writes — the circle
  service has no override-audit gate (unlike parent-student/enrollment).

## 4. Flow — normal and logical

- Mosque admin creates a mosque → adds a teacher → creates a circle
  (`PLANNING` → `ACTIVE`) → enrolls students → `ENDED` when the program
  concludes. TEACHER sees only own circles; mosque staff/students see the
  mosque's circles.

## 5. Role UI (frontend)

- MOSQUE_ADMIN / SUPER_ADMIN: circle management (create / edit / deactivate).
- TEACHER: own circles view + schedule.
- STUDENT/PARENT: read-only circle listings where exposed (illegal CTAs absent).

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. For
manual-only cases use:

| # | Endpoint | Role | Input | Expected | Actual | DB after | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | POST /circles | SUPER_ADMIN | `{mosqueId,teacherId,name,level,type}` | 201 | | | ✅/❌ |

## Known gaps to flag

- (a) TEACHER `GET /circles` throws **404** (`ResourceNotFoundException`)
  instead of an empty 200 when the teacher has no profile.
- (b) Circle writes have **no override-audit gate**: SUPER_ADMIN create/update/
  delete succeed without an audit reason — inconsistent with parent-student and
  enrollment write paths.
- (c) Success paths need an existing mosque + teacher (absent from seed data) —
  the script SKIPs them until such data exists.
