# Workspace API — Testing Guide

Step-by-step manual test plan for **`/api/v1/me`** (authenticated user's
workspace context), mapped to the project's testing considerations:

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
- Tokens per role: login as SUPER_ADMIN, MOSQUE_ADMIN, TEACHER, STUDENT, PARENT.
- **Important:** the seed users only carry a `users` row — none have profile
  wiring (no `mosque_admins`/`teachers`/`students`/`parent_student` rows). So
  with seed data only, SUPER_ADMIN returns 200 and the other roles return 404.
- To exercise real 200 responses for non-super roles, `run-tests.ps1` registers
  **throwaway users** and wires them (throwaway mosque + profile) — see Step 2.
- DBeaver open on the `darb` DB.

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/me/profile` | any authenticated (`@PreAuthorize("isAuthenticated()")`) |

No path/query parameters, no request body → no 400 cases. No role restrictions
→ no 403 cases. 404 appears when an authenticated user has no profile wiring.

## 2. Step-by-step

**Step 1 — no token / garbage token:** GET `/me/profile` without a token → **401**.
Garbage bearer token → **401**.

**Step 2 — role resolution (response per role).** The response is
`WorkspaceProfileResponse`: `profileId`, `mosqueId`, `teacherId`, `studentId`,
`parentStudentIds`, `membershipStatus`, `mosqueName`, `pendingMosqueName`.

- SUPER_ADMIN → **200**, `profileId = userId`, `membershipStatus = ASSIGNED`,
  no `mosqueId`.
- MOSQUE_ADMIN with a `mosque_admins` row → **200** with `mosqueId`,
  `mosqueName`, `membershipStatus = ASSIGNED`; without one → **404**
  (`WorkspaceService.fromMosqueAdmin`).
- TEACHER with a `teachers` row → **200** (`teacherId`, `mosqueId`); with only
  a PENDING `mosque_join_requests` row → **200** with
  `membershipStatus = PENDING` and `pendingMosqueName`; with neither → **404**.
- STUDENT with a `students` row → **200** (`studentId`, `mosqueId`); pending
  join → **200 PENDING**; neither → **404**.
- PARENT with `parent_student` links → **200** with `parentStudentIds` (no
  `mosqueId` — PARENT is link-scoped); no links → **404**.

**Step 3 — wired throwaway roles:** the script registers throwaway users and
wires profiles through the standard admin APIs (mosque create, student/teacher
create, mosque-admin assign, parent-student link) so each non-super role gets a
real 200. If any wiring step fails (e.g. `-ReadOnly`), those cases are
recorded as SKIPPED.

## 3. DB changes (DBeaver)

- **No writes** — this resource is read-only (`@Transactional(readOnly = true)`
  in `WorkspaceService`).
- Read source tables: `users`, `mosque_admins`, `teachers`, `students`,
  `parent_student`, `mosque_join_requests` (PENDING lookup for student/teacher).
- To verify the 404s: confirm the seed users have **no** rows in
  `mosque_admins`/`teachers`/`students`/`parent_student`.

## 4. Flow — normal and logical

- Frontend routing: after login the SPA calls `/me/profile` to decide which
  workspace to render; a 404 here means "profile not wired" (the app should
  send the user to onboarding/join flows instead of an error screen).
- SUPER_ADMIN lands on the fleet hub (no mosque context); MOSQUE_ADMIN/TEACHER/
  STUDENT land on their mosque workspace; PARENT lands on the children view.
- Pending join: student/teacher who requested to join a mosque see the mosque
  name and a PENDING state until the admin approves/rejects.

## 5. Role UI (frontend)

Every role uses this endpoint as their workspace bootstrap. There are no
management controls here; it only feeds the layout. Super admin fleet view,
mosque admin dashboard, teacher/student mosque views, parent children view —
all keyed off this one payload.

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. For
manual-only cases use:

| # | Endpoint | Role | Input | Expected | Actual | DB after | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | GET /me/profile | TEACHER (wired) | — | 200, teacherId set | | | ✅/❌ |

## Known gaps to flag

- (a) Seed data provides no profile wiring, so MOSQUE_ADMIN/TEACHER/STUDENT/
  PARENT hit 404 out of the box — this is seed-data shape, not a defect, but
  easy to misread as an outage.
- (b) MOSQUE_ADMIN/TEACHER/STUDENT resolve only the **first** matching profile
  (`stream().findFirst()`); a user with multiple profiles returns the first.
- No role-enforcement or validation gaps observed on this endpoint.
