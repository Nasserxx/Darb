# Student API — Testing Guide

Step-by-step manual test plan for **`/api/v1/students`** (student profiles,
onboard/join, management), mapped to the project's testing considerations:

1. Press everything
2. Test logic
3. DB changes
4. Flow — normal and logical
5. Role UI
6. Results documentation

Automated matrix: run `.\run-tests.ps1` in this folder (prints results table +
`results.csv`).

## 0. Preconditions

- Backend running (port 8089) with the 5 seed users. The seeded STUDENT has **no
  student profile** — it drives the onboard/eligibility and 403 cases.
- Throwaway users: the script registers a fresh cohort (`st.*@darb.app`,
  timestamp-suffixed): a MOSQUE_ADMIN (onboards a throwaway mosque), a STUDENT
  (onboards as student), plus a second STUDENT for the join-via-invite-code
  path.
- Tokens per role: login as SUPER_ADMIN, MOSQUE_ADMIN, TEACHER, STUDENT, PARENT.
- DBeaver open on the `darb` DB.

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/students` | SUPER_ADMIN, MOSQUE_ADMIN, TEACHER |
| 2 | GET | `/api/v1/students/search` | MOSQUE_ADMIN |
| 3 | GET | `/api/v1/students/{id}` | SUPER_ADMIN, MOSQUE_ADMIN, TEACHER, STUDENT (self), PARENT (linked) |
| 4 | POST | `/api/v1/students/onboard` | STUDENT |
| 5 | POST | `/api/v1/students/join` | STUDENT |
| 6 | POST | `/api/v1/students` | SUPER_ADMIN, MOSQUE_ADMIN |
| 7 | PUT | `/api/v1/students/{id}` | SUPER_ADMIN, MOSQUE_ADMIN |
| 8 | DELETE | `/api/v1/students/{id}` | SUPER_ADMIN, MOSQUE_ADMIN |

Hit every endpoint per allowed role, plus no token (401) and a garbage token (401).

## 2. Step-by-step

**Step 1 — Auth basics:** no token / garbage token → 401 on GET `/students`.

**Step 2 — GET `/students` (pagination):** SUPER_ADMIN → 200 (all). Assigned
MOSQUE_ADMIN → 200 (scoped). TEACHER → 200 **empty** page (`pageForCaller`;
unassigned seed teacher). STUDENT → 403. PARENT → 403.

**Step 3 — GET `/students/search` (MOSQUE_ADMIN):** assigned MOSQUE_ADMIN with
`q` → 200. Seed MOSQUE_ADMIN (no assignment) → 403. SUPER_ADMIN/TEACHER/STUDENT
→ 403. No `q` → 400.

**Step 4 — GET `/students/{id}`:** SUPER_ADMIN → 200 (any). Assigned
MOSQUE_ADMIN → 200 (same mosque). Assigned TEACHER → 200 (same mosque). Seed
STUDENT with profile → 200 (self). Seed STUDENT without profile → 404. Other
mosque admin/teacher → 403. Seed MOSQUE_ADMIN (no assignment) → 403. PARENT
without linked student → 403. Invalid UUID → 400; random UUID → 404.

**Step 5 — POST `/students/onboard` (STUDENT):** seed STUDENT (no profile) →
201 (creates profile for the seed mosque). Onboard again (already has profile) →
403 `"Student profile already exists"`. TEACHER/MOSQUE_ADMIN/SUPER_ADMIN → 403.
Missing `mosqueId` → 400. Unknown `mosqueId` → 404. Inactive mosque → 400.
Note: onboarding the **seed** student mutates seed data — verify `version` and
the student row in DBeaver; this is the app's own demo flow.

**Step 6 — POST `/students/join` (STUDENT):** with a valid student invite code →
201 (profile created). Invalid code → 404. Blank → 400. Already-joined STUDENT →
403. TEACHER → 403.

**Step 7 — POST `/students` (SUPER_ADMIN, MOSQUE_ADMIN):** body
`StudentCreateRequest` (`userId`, `mosqueId`) → 201. Nonexistent `userId` /
`mosqueId` → 404. Missing `userId` → 400. TEACHER/STUDENT → 403.

**Step 8 — PUT `/students/{id}`:** SUPER_ADMIN and MOSQUE_ADMIN (same mosque) →
200. TEACHER → 403. STUDENT → 403. Random UUID → 404.

**Step 9 — DELETE `/students/{id}`:** SUPER_ADMIN and MOSQUE_ADMIN (same mosque)
→ 200. TEACHER → 403. STUDENT → 403. Random UUID → 404; invalid UUID → 400.

## 3. DB changes (DBeaver)

- `POST /students/onboard` → new row in `students` (`user_id`, `mosque_id`,
  `created_at`; unique index on `user_id` prevents a second profile). The mosque
  binding lives on the `students` row itself (users table has no `mosque_id`
  for STUDENT).
- `POST /students/join` → new `students` row via invite-code lookup.
- `POST /students` → new `students` row (SUPER_ADMIN/MOSQUE_ADMIN path).
- `PUT /students/{id}` → updated `students` fields, `version` bumped.
- `DELETE /students/{id}` → `students` row removed (physical delete).

## 4. Flow — normal and logical

- Student signup: register as STUDENT → `POST /students/onboard` with the
  mosque id (invited via `member-join/preview`) OR `POST /students/join` with a
  student invite code. Once a profile exists, the student is visible to the
  mosque admin and teachers and can be linked to a parent.
- An already-profiled student cannot onboard/join again → 403 (single profile
  per user, enforced by unique index + service check).
- Mosque admin manages the roster via create/update/delete; SUPER_ADMIN too.

## 5. Role UI (frontend)

- STUDENT: onboarding/join forms (eligibility gating shows only when no
  profile), then a student dashboard.
- MOSQUE_ADMIN: student roster screen with add/edit/remove.
- TEACHER: read-only view of students (roster/dropdown for circles).
- SUPER_ADMIN: same roster management across mosques.

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. For
manual-only cases use:

| # | Endpoint | Role | Input | Expected | Actual | DB after | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | POST /students/onboard | STUDENT | mosqueId | 201 | | students row | ✅/❌ |

## Known gaps to flag

- Seed STUDENT onboarding mutates seed data (creates a profile the seed user did
  not have) — subsequent runs still pass (idempotent 403 on re-onboard) but the
  DB ends up with a real profile for `student@darb.app`.
- Students cannot leave/delete their own profile; only an admin (or SUPER_ADMIN)
  can remove them — no student-initiated "leave mosque" action.
- `pageForCaller` returns an empty page (200) for an unassigned MOSQUE_ADMIN /
  unprofiled TEACHER instead of an error — "no data" and "no assignment/profile"
  are indistinguishable in the response.
- No check observed that a student's `mosque_id` (on the `students` row) matches
  the mosque used on create — confirm the service guards cross-mosque creation.
