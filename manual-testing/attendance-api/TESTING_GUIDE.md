# Attendance API — Testing Guide

Step-by-step manual test plan for **`/api/v1/attendance`** (Attendance
Management: recording and tracking student attendance at circle sessions),
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
- **Domain data required for successful writes**: attendance records reference an
  **Enrollment** (`enrollmentId`), a **Circle** (`circleId`) and a **User**
  (`recordedBy`, set from the caller). The seed users have **no** mosque /
  student / circle / enrollment / parent-link profiles, so on a fresh DB most
  create/excuse cases cannot succeed — the script resolves IDs dynamically from
  the students/circles/enrollments/parent-students lists and marks cases
  `SKIPPED` when prerequisites are missing.
- Tokens per role: login as SUPER_ADMIN, MOSQUE_ADMIN, TEACHER, STUDENT, PARENT.
- DBeaver open on the `darb` DB (`attendance`, `override_audit_log` tables).

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/attendance` | SUPER_ADMIN, MOSQUE_ADMIN, TEACHER |
| 2 | GET | `/api/v1/attendance/circle/{circleId}` | any authenticated (mosque access enforced) |
| 3 | GET | `/api/v1/attendance/student/{studentId}` | any authenticated (student access enforced) |
| 4 | GET | `/api/v1/attendance/{id}` | any authenticated (student access enforced) |
| 5 | POST | `/api/v1/attendance` | SUPER_ADMIN, MOSQUE_ADMIN, TEACHER (assigned to circle) |
| 6 | PUT | `/api/v1/attendance/{id}` | SUPER_ADMIN, MOSQUE_ADMIN, TEACHER (SUPER_ADMIN needs audit reason) |
| 7 | POST | `/api/v1/attendance/{id}/excuse` | STUDENT, PARENT (linked child) |

**Domain verbs (verify):** *Mark* = TEACHER (assigned circle) /
MOSQUE_ADMIN / SUPER_ADMIN; *Excuse* = PARENT (linked child) — code also
allows STUDENT for their own records (`@PreAuthorize("hasAnyRole('STUDENT','PARENT')")`).

Enums:
- `AttendanceStatus`: `PRESENT`, `LATE`, `ABSENT`, `EXCUSED`, `HOLIDAY`.
- `AbsenceReason`: `SICK`, `FAMILY`, `TRAVEL`, `PERSONAL`, `OTHER`.

Hit every endpoint per allowed role, plus no token (401), garbage token (401),
invalid enum (400), and nonexistent id (404).

## 2. Step-by-step

**Step 1 — Auth basics:** no token → 401 on all 7. Garbage token → 401.

**Step 2 — GET `/attendance` (list):** SUPER_ADMIN → 200 (all tenants);
MOSQUE_ADMIN/TEACHER → 200 (mosque-scoped, empty without assignment);
STUDENT/PARENT → 403.

**Step 3 — GET `/attendance/circle/{circleId}`:** with a real circle → SUPER_ADMIN
200; STUDENT (no mosque assignment) → 403 (effective access check, despite
"any authenticated"). Random UUID → 404. Invalid UUID → 400.

**Step 4 — GET `/attendance/student/{studentId}`:** with a real student →
SUPER_ADMIN 200; PARENT without link → 403. Random UUID → 404. Invalid UUID → 400.

**Step 5 — POST `/attendance` (Mark):** SUPER_ADMIN/MOSQUE_ADMIN with an
ACTIVE enrollment that belongs to the circle → 201 (DB checks: enrollment
exists → 404; circle exists → 404; enrollment.circle != circle → 400;
enrollment not ACTIVE → 400). TEACHER **not** assigned to the circle → 403.
STUDENT/PARENT → 403. Empty body → 400; `status="MAYBE"` → 400. Random
enrollmentId → 404.

**Step 6 — GET `/attendance/{id}`:** existing record → SUPER_ADMIN 200, TEACHER
without mosque → 403. Random UUID → 404. Invalid UUID → 400.

**Step 7 — PUT `/attendance/{id}`:** SUPER_ADMIN **without** audit reason → 400
(`X-Audit-Reason` header or `auditReason` body, min 8 chars, writes
`override_audit_log`). SUPER_ADMIN with audit reason → 200. TEACHER/MOSQUE_ADMIN
need mosque access (seed roles without assignment → 403). STUDENT → 403.
Nonexistent id (with audit reason) → 404.

**Step 8 — POST `/attendance/{id}/excuse` (PARENT flow):** TEACHER /
MOSQUE_ADMIN / SUPER_ADMIN → 403 (only STUDENT/PARENT). PARENT on a record of a
**non-linked** student → 403. PARENT on a **linked child's** record → 200,
record flips to `status=EXCUSED`. STUDENT on own record → 200. Nonexistent id →
404. `absenceReason` > 500 chars → 400. **Note:** `absenceReason` here is a
free-text `String`; unknown values are silently mapped to `AbsenceReason.OTHER`
(not a 400).

## 3. DB changes (DBeaver)

- `POST /attendance` → row in `attendance` (`enrollment_id`, `circle_id`,
  `session_date`, `status`, `scheduled_start`, `recorded_by`).
- `PUT /attendance/{id}` as SUPER_ADMIN → `status`/`actual_check_in`/
  `minutes_late`/`absence_reason` updated **and** a row in
  `override_audit_log` (`action='ATTENDANCE_UPDATE'`). Non-super updates write
  no audit row.
- `POST /attendance/{id}/excuse` → `status='EXCUSED'`, `absence_reason`,
  `excuse_document_url` set; `recorded_by` untouched.
- There is **no DELETE endpoint** (controller has none; `AttendanceService.delete`
  is not exposed) — records can only be excused, not removed via API.

## 4. Flow — normal and logical

- Happy path: TEACHER marks attendance (PRESENT/LATE) → PARENT excuses an ABSENT
  record for the linked child → status flips to EXCUSED → student/parent see the
  updated record via `GET /attendance/student/{studentId}`.
- Logic checks worth re-testing by hand: enrollment-circles mismatch (400),
  non-ACTIVE enrollment (400), TEACHER not assigned to circle (403), SUPER_ADMIN
  update without audit reason (400).

## 5. Role UI (frontend)

- TEACHER (assigned circle): Mark / update attendance in the session sheet.
- PARENT: "Excuse absence" on the child's attendance view (CTAs only for their
  own children — illegal CTAs absent).
- STUDENT: view own attendance; excuse own record if enabled.
- SUPER_ADMIN/MOSQUE_ADMIN: overview lists; MOSQUE_ADMIN tenant-scoped.

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. For
manual-only cases (needs a linked parent / active enrollment) use:

| # | Endpoint | Role | Input | Expected | Actual | DB after | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | POST /attendance | TEACHER | ACTIVE enrollment | 201 | | | ✅/❌ |
| 2 | POST /attendance/{id}/excuse | PARENT | linked child record | 200, EXCUSED | | | ✅/❌ |

## Known gaps to flag

- (a) `scheduled_start` is `NOT NULL` in the DB but optional in
  `AttendanceCreateRequest` — omitting it causes a DB error (500), not a 400.
- (b) `AttendanceService.delete` exists but has no controller mapping (no
  delete at all).
- (c) `POST /attendance` performs **no tenant/mosque assertion** on the
  enrollment (only the circle-teacher assignment check for TEACHER), so any
  MOSQUE_ADMIN/SUPER_ADMIN can mark attendance for any enrollment.
- (d) Excuse `absenceReason` is a free-text string; typos silently become
  `AbsenceReason.OTHER` instead of a 400.
