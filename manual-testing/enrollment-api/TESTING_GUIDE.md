# Enrollment API — Testing Guide

Step-by-step manual test plan for **`/api/v1/enrollments`** (student enrollment
in study circles), mapped to the project's testing considerations:

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
- An existing **student + circle** are required for the create/update success
  paths. The script resolves them dynamically from `GET /students` and
  `GET /circles` (SUPER_ADMIN); on a clean seed DB those cases are SKIPPED.
- The script creates a **throwaway enrollment** (SUPER_ADMIN) for update tests.
  There is no DELETE endpoint, so created rows are left as throwaway data.
- Tokens per role: login as SUPER_ADMIN, MOSQUE_ADMIN, TEACHER, STUDENT, PARENT.
- DBeaver open on the `darb` DB.

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/enrollments` | SUPER_ADMIN, MOSQUE_ADMIN, TEACHER |
| 2 | GET | `/api/v1/enrollments/student/{studentId}` | any authenticated (student-scoped) |
| 3 | GET | `/api/v1/enrollments/{id}` | any authenticated (student-scoped) |
| 4 | POST | `/api/v1/enrollments` | SUPER_ADMIN, MOSQUE_ADMIN |
| 5 | PUT | `/api/v1/enrollments/{id}` | SUPER_ADMIN, MOSQUE_ADMIN |

There is **no DELETE route** — `EnrollmentService.delete()` exists but the
controller does not expose it (see Known gaps (b)); withdrawal happens via
`PUT` with `status=WITHDRAWN`.

Hit every endpoint per allowed role, plus no token (401) and a garbage token (401).

## 2. Step-by-step

**Step 1 — Auth basics:** no token → 401 on list/student-list/get/create/update.
Garbage token → 401.

**Step 2 — GET `/enrollments`:** SUPER_ADMIN → 200 (all, global). MOSQUE_ADMIN /
TEACHER → 200 (mosque-scoped; empty page without an assignment). STUDENT/PARENT
→ 403.

**Step 3 — GET `/enrollments/student/{studentId}`:** invalid UUID → 400. Random
UUID → 404. Existing student → SUPER_ADMIN 200; STUDENT self → 200, other → 403;
PARENT linked child → 200, unlinked → 403; TEACHER/MOSQUE_ADMIN → 200 within the
same mosque, else 403.

**Step 4 — GET `/enrollments/{id}`:** invalid UUID → 400. Random UUID → 404.
Existing enrollment → SUPER_ADMIN 200; PARENT of a linked child → 200 else 403;
STUDENT self → 200 else 403; TEACHER/MOSQUE_ADMIN same mosque → 200 else 403.

**Step 5 — POST `/enrollments`:** empty body → 400 (`studentId`, `circleId`
required). Unknown student/circle → 404. Student and circle in **different
mosques** → 400. `approvedBy` is taken from the token (body value ignored).
TEACHER/STUDENT/PARENT → 403. Success → 201 (status defaults to `PENDING`).
Note Known gap (a): MOSQUE_ADMIN create has no mosque-scope check.

**Step 6 — PUT `/enrollments/{id}`:** invalid body / UUID → 400. SUPER_ADMIN
without audit reason → 400 (an `ENROLLMENT_STATUS_FORCE` audit row is written
when a SUPER_ADMIN changes status); random UUID with audit → 404. Update `status`
/ `withdrawnDate` / `notes` → 200 (withdraw via `status=WITHDRAWN`).
MOSQUE_ADMIN → 200 within mosque, else 403. TEACHER/STUDENT/PARENT → 403.

## 3. DB changes (DBeaver)

- `enrollments` rows: `student_id`, `circle_id`, `status`, `enrolled_date`,
  `withdrawn_date`, `approved_by`, `notes`, timestamps.
- POST → new row (`status` defaults `PENDING`, `enrolled_date` defaults today,
  `approved_by` = caller). PUT → `status` / `withdrawn_date` / `notes` updates.
- No physical delete; withdrawing = `PUT` with `status=WITHDRAWN` (+ optional
  `withdrawnDate`).
- **Audit logic:** `override_audit_log` gets an `ENROLLMENT_STATUS_FORCE` row
  when a SUPER_ADMIN changes status with an audit reason; without one the update
  is rejected (400).

## 4. Flow — normal and logical

- Admin creates a circle → enrolls a student (`PENDING`) → approves to `ACTIVE`
  → student attends → `COMPLETED` or `WITHDRAWN`. A parent of the linked student
  reads the child's enrollments via `/enrollments/student/{childId}`.
- Enrollment requires student and circle in the **same mosque** (400 otherwise).

## 5. Role UI (frontend)

- MOSQUE_ADMIN / SUPER_ADMIN: manage enrollments (create / approve / withdraw).
- TEACHER: class rosters per circle (read view).
- STUDENT/PARENT: read-only enrollment views for themselves / their child.

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. For
manual-only cases use:

| # | Endpoint | Role | Input | Expected | Actual | DB after | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | POST /enrollments | SUPER_ADMIN | `{studentId,circleId}` | 201 | | | ✅/❌ |

## Known gaps to flag

- (a) `EnrollmentService.create` has **no mosque-scope assertion** — a
  MOSQUE_ADMIN can enroll any student+circle as long as the student and circle
  are in the same mosque (which may be outside the admin's mosque).
- (b) **No DELETE endpoint** even though `EnrollmentService.delete()` exists —
  withdrawal is only possible via PUT status change.
- (c) Success paths need existing student + circle (absent from seed data) — the
  script SKIPs them until such data exists.
