# Parent-Student API — Testing Guide

Step-by-step manual test plan for **`/api/v1/parent-students`** (parent <-> student
guardianship links and notification preferences), mapped to the project's testing
considerations:

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
- A **student profile with a `parentInviteCode`** is needed for the join-flow
  success paths. Seed users have no student data, so the script resolves any
  existing student from `GET /students` and, if one exists without a code,
  assigns a throwaway `parentInviteCode` via SUPER_ADMIN `PUT /students/{id}`.
  On a clean DB the join success cases are SKIPPED.
- The script registers a **throwaway PARENT user** (`test.parent@darb.app`) for
  admin-created links, so seed accounts are never destructive targets.
- Tokens per role: login as SUPER_ADMIN, MOSQUE_ADMIN, TEACHER, STUDENT, PARENT.
- DBeaver open on the `darb` DB.

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/parent-students` | SUPER_ADMIN, MOSQUE_ADMIN, PARENT |
| 2 | GET | `/api/v1/parent-students/{id}` | SUPER_ADMIN, MOSQUE_ADMIN, PARENT |
| 3 | POST | `/api/v1/parent-students` | SUPER_ADMIN, MOSQUE_ADMIN |
| 4 | GET | `/api/v1/parent-students/join/preview?code=` | PARENT |
| 5 | POST | `/api/v1/parent-students/join` | PARENT |
| 6 | GET | `/api/v1/parent-students/my-children` | PARENT |
| 7 | PUT | `/api/v1/parent-students/{id}` | SUPER_ADMIN, MOSQUE_ADMIN |
| 8 | DELETE | `/api/v1/parent-students/{id}` | SUPER_ADMIN |

Hit every endpoint per allowed role, plus no token (401) and a garbage token (401).

## 2. Step-by-step

**Step 1 — Auth basics:** no token → 401 on list and create. Garbage token → 401.

**Step 2 — GET `/parent-students`:** SUPER_ADMIN → 200 (all links, global);
MOSQUE_ADMIN → 200 (mosque-scoped; empty page without a mosque assignment);
PARENT → 200 but an **empty page** (see Known gaps (a)); TEACHER/STUDENT → 403.

**Step 3 — POST `/parent-students` (admin-created link):** empty body → 400
(`parentUserId` / `studentId` required). SUPER_ADMIN without an audit reason
(body `auditReason` or `X-Audit-Reason` header, min 8 chars) → 400. Unknown
parent/student id → 404. MOSQUE_ADMIN → 201 with an existing parent + student
(no audit needed; note Known gap (b)). TEACHER/STUDENT/PARENT → 403. On success
the link appears in GET `/parent-students`.

**Step 4 — Parent join flow (the link parents actually use):** the student's
`parentInviteCode` is set on the student profile (create/update). PARENT calls
GET `/join/preview?code=` → 200 `{mosqueName, studentName}`; invalid code → 404;
missing `code` → 400; non-PARENT → 403. Then POST `/join` `{inviteCode}` → 201
(relationship defaults to `parent`, primary, notifications on). The same code
again → 400 "already linked". Empty body → 400; invalid code → 404; non-PARENT
→ 403. GET `/my-children` → 200 list of linked students; non-PARENT → 403.

**Step 5 — GET `/parent-students/{id}`:** invalid UUID → 400. Random UUID → 404.
PARENT on an own link → 200; PARENT on another parent's link → 403. MOSQUE_ADMIN
→ 200 within their mosque, else 403. TEACHER/STUDENT → 403.

**Step 6 — PUT `/parent-students/{id}`:** invalid body / UUID → 400. SUPER_ADMIN
without audit reason → 400; random UUID with audit → 404. Update `relationship` /
`isPrimary` / `receivesNotifications` → 200 (SUPER_ADMIN needs an audit reason).
TEACHER/STUDENT/PARENT → 403. MOSQUE_ADMIN → 200 within mosque, else 403.

**Step 7 — DELETE `/parent-students/{id}` (SUPER_ADMIN only):** no audit reason →
400; random UUID with `X-Audit-Reason` → 404; success → 200 and the row is
**hard-deleted**. All other roles → 403.

## 3. DB changes (DBeaver)

- `parent_student` rows: `parent_user_id`, `student_id`, `mosque_id` (copied
  from the student), `relationship`, `is_primary`, `receives_notifications`,
  `created_at` / `updated_at`.
- POST create / join → new row. PUT → field updates. DELETE → row removed
  (physical delete, not a status flip).
- **Audit logic:** SUPER_ADMIN POST/PUT/DELETE write `override_audit_log`
  (`PARENT_STUDENT_CREATE` / `PARENT_STUDENT_UPDATE` / `PARENT_STUDENT_DELETE`)
  when an audit reason is supplied; without one the write is rejected (400).
  MOSQUE_ADMIN writes leave no audit row.
- The `students.parent_invite_code` column is what the join flow matches.

## 4. Flow — normal and logical

- Admin creates a student and sets `parentInviteCode`. Parent logs in → previews
  the code (sees mosque + student name) → joins → `/my-children` lists the
  student → parent now reads the child through student-scoped APIs (e.g.
  `GET /enrollments/student/{childId}`).
- Re-joining the same student → 400 "already linked" (idempotency guard).
- Deleting the link (SUPER_ADMIN) immediately removes the parent's access.

## 5. Role UI (frontend)

- PARENT: "My children" view, join-by-code entry, notification toggles.
- MOSQUE_ADMIN / SUPER_ADMIN: link management UI (create / edit / remove links).
- TEACHER/STUDENT: no entry points for this resource (illegal CTAs absent).

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. For
manual-only cases use:

| # | Endpoint | Role | Input | Expected | Actual | DB after | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | POST /parent-students/join | PARENT | `{inviteCode}` | 201 | | | ✅/❌ |

## Known gaps to flag

- (a) PARENT `GET /parent-students` returns an **empty page** — the service uses
  `pageForCaller` which yields `Page.empty` for PARENT, so parents never see
  their own links here (contradicts the endpoint description).
- (b) `ParentStudentService.create` performs **no mosque-scope assertion** — any
  MOSQUE_ADMIN can link any student regardless of mosque.
- (c) No endpoint generates/rotates a student's `parentInviteCode`; it is set
  only through student create/update.
- (d) Join success paths cannot run on a clean seed DB (no student with an
  invite code) — the script SKIPs them; manual setup (create student + set code)
  is required to exercise them.
