# Teacher API — Testing Guide

Step-by-step manual test plan for **`/api/v1/teachers`** (teacher profiles,
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

- Backend running (port 8089) with the 5 seed users. The seeded TEACHER has **no
  teacher profile** (no mosque assignment) — it drives the onboard/eligibility
  and 403 cases.
- Throwaway users: the script registers a fresh cohort (`te.*@darb.app`,
  timestamp-suffixed): a MOSQUE_ADMIN (onboards a throwaway mosque), a TEACHER
  (onboards as teacher), plus a second TEACHER for the join-via-invite-code
  path, and a STUDENT for the negative cases.
- Tokens per role: login as SUPER_ADMIN, MOSQUE_ADMIN, TEACHER, STUDENT, PARENT.
- DBeaver open on the `darb` DB.

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/teachers` | SUPER_ADMIN, MOSQUE_ADMIN |
| 2 | GET | `/api/v1/teachers/search` | MOSQUE_ADMIN |
| 3 | GET | `/api/v1/teachers/{id}` | SUPER_ADMIN, MOSQUE_ADMIN, TEACHER (self) |
| 4 | POST | `/api/v1/teachers/onboard` | TEACHER |
| 5 | POST | `/api/v1/teachers/join` | TEACHER |
| 6 | POST | `/api/v1/teachers` | SUPER_ADMIN, MOSQUE_ADMIN |
| 7 | PUT | `/api/v1/teachers/{id}` | SUPER_ADMIN, MOSQUE_ADMIN |
| 8 | DELETE | `/api/v1/teachers/{id}` | SUPER_ADMIN, MOSQUE_ADMIN |

Hit every endpoint per allowed role, plus no token (401) and a garbage token (401).

## 2. Step-by-step

**Step 1 — Auth basics:** no token / garbage token → 401 on GET `/teachers`.

**Step 2 — GET `/teachers` (pagination):** SUPER_ADMIN → 200 (all). Assigned
MOSQUE_ADMIN → 200 (scoped). Seed MOSQUE_ADMIN (no assignment) → 200 **empty**
page. Seed TEACHER (no profile) → 200 **empty** page (`pageForCaller`). STUDENT
→ 403.

**Step 3 — GET `/teachers/search` (MOSQUE_ADMIN):** assigned MOSQUE_ADMIN with
`q` → 200. Seed MOSQUE_ADMIN (no assignment) → 403. SUPER_ADMIN/TEACHER/STUDENT
→ 403. No `q` → 400.

**Step 4 — GET `/teachers/{id}`:** SUPER_ADMIN → 200 (any). Assigned
MOSQUE_ADMIN → 200 (same mosque). Seed TEACHER with profile → 200 (self). Seed
TEACHER without profile → 404. Other mosque admin → 403. Seed MOSQUE_ADMIN (no
assignment) → 403. Invalid UUID → 400; random UUID → 404.

**Step 5 — POST `/teachers/onboard` (TEACHER):** seed TEACHER (no profile) →
201 (creates profile for the seed mosque). Onboard again (already has profile) →
403 `"Teacher profile already exists"`. STUDENT/MOSQUE_ADMIN/SUPER_ADMIN → 403.
Missing `mosqueId` → 400. Unknown `mosqueId` → 404. Inactive mosque → 400.
Note: onboarding the **seed** teacher mutates seed data — verify `version` and
the teacher row in DBeaver; this is the only mutation touching seed data, and it
is what the app's own demo flow does.

**Step 6 — POST `/teachers/join` (TEACHER):** with a valid teacher invite code →
201 (profile created). Invalid code → 404. Blank → 400. Already-joined TEACHER →
403. STUDENT → 403.

**Step 7 — POST `/teachers` (SUPER_ADMIN, MOSQUE_ADMIN):** body
`TeacherCreateRequest` (`userId`, `mosqueId`) → 201. Nonexistent `userId` /
`mosqueId` → 404. Missing `userId` → 400. STUDENT/TEACHER → 403.

**Step 8 — PUT `/teachers/{id}`:** SUPER_ADMIN and MOSQUE_ADMIN (same mosque) →
200. TEACHER (non-self) → 403. STUDENT → 403. Random UUID → 404.

**Step 9 — DELETE `/teachers/{id}`:** SUPER_ADMIN and MOSQUE_ADMIN (same mosque)
→ 200. TEACHER → 403 (teachers cannot delete profiles, even their own — no
self-delete endpoint). Random UUID → 404. Invalid UUID → 400.

## 3. DB changes (DBeaver)

- `POST /teachers/onboard` → new row in `teachers` (`user_id`, `mosque_id`,
  `created_at`; unique index on `user_id` prevents a second profile). Also sets
  the user's `mosque_id`? **No** — the mosque binding lives on the `teachers`
  row itself (users table has no `mosque_id` for TEACHER).
- `POST /teachers/join` → new `teachers` row via invite-code lookup.
- `POST /teachers` → new `teachers` row (SUPER_ADMIN/MOSQUE_ADMIN path).
- `PUT /teachers/{id}` → updated `teachers` fields, `version` bumped.
- `DELETE /teachers/{id}` → `teachers` row removed; no soft-delete column
  observed — physical delete.

## 4. Flow — normal and logical

- Teacher signup: register as TEACHER → `POST /teachers/onboard` with the
  mosque id (invited via `member-join/preview`) OR `POST /teachers/join` with a
  teacher invite code. Once a profile exists, the teacher is visible to the
  mosque admin and can be assigned to circles.
- An already-profiled teacher cannot onboard/join again → 403 (single profile
  per user, enforced by unique index + service check).
- Mosque admin manages the roster via create/update/delete; SUPER_ADMIN can too.

## 5. Role UI (frontend)

- TEACHER: onboarding/join forms (eligibility gating shows only when no
  profile), then a teacher dashboard.
- MOSQUE_ADMIN: teacher roster screen with add/edit/remove.
- SUPER_ADMIN: same roster management across mosques.

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. For
manual-only cases use:

| # | Endpoint | Role | Input | Expected | Actual | DB after | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | POST /teachers/onboard | TEACHER | mosqueId | 201 | | teachers row | ✅/❌ |

## Known gaps to flag

- Seed TEACHER onboarding mutates seed data (creates a profile the seed
  user did not have) — subsequent runs still pass (idempotent 403 on re-onboard)
  but the DB ends up with a real profile for `teacher@darb.app`.
- Teachers cannot leave/delete their own profile; only an admin (or SUPER_ADMIN)
  can remove them — the UI has no teacher-initiated "leave mosque" action.
- `pageForCaller` returns an empty page (200) for an unassigned MOSQUE_ADMIN /
  unprofiled TEACHER instead of an error — "no data" and "no assignment/profile"
  are indistinguishable in the response.
- No check observed that a teacher's `mosque_id` (on the `teachers` row) matches
  the mosque used on create — a MOSQUE_ADMIN of mosque A might create a teacher
  row bound to mosque B; confirm the service guards this.
