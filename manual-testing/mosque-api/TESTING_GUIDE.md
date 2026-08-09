# Mosque API — Testing Guide

Step-by-step manual test plan for **`/api/v1/mosques`** (Mosque Management,
onboarding, invite codes, join requests), mapped to the project's testing
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

- Backend running (port 8089) with the 5 seed users. **Important:** the seeded
  MOSQUE_ADMIN/TEACHER/STUDENT have **no mosque assignment or profile**.
- Throwaway users: the script registers a fresh cohort (`mq.*@darb.app`,
  timestamp-suffixed) each run — a throwaway MOSQUE_ADMIN onboards a throwaway
  mosque, and throwaway TEACHER/STUDENT drive the join-request flow. Seed users
  are never assigned. Older cohorts are orphans in the `users` table (harmless).
- Tokens per role: login as SUPER_ADMIN, MOSQUE_ADMIN, TEACHER, STUDENT, PARENT.
- DBeaver open on the `darb` DB.

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/mosques` | any authenticated |
| 2 | GET | `/api/v1/mosques/{id}` | any authenticated (same-mosque scoped) |
| 3 | GET | `/api/v1/mosques/search?q=&city=` | TEACHER, STUDENT |
| 4 | GET | `/api/v1/mosques/member-join/preview?code=&role=` | TEACHER, STUDENT |
| 5 | GET | `/api/v1/mosques/invite-codes` | MOSQUE_ADMIN |
| 6 | GET | `/api/v1/mosques/join/preview?code=` | MOSQUE_ADMIN |
| 7 | POST | `/api/v1/mosques/onboard` | MOSQUE_ADMIN |
| 8 | POST | `/api/v1/mosques/join` | MOSQUE_ADMIN |
| 9 | POST | `/api/v1/mosques/join-requests` | TEACHER, STUDENT |
| 10 | DELETE | `/api/v1/mosques/join-requests/my` | TEACHER, STUDENT |
| 11 | POST | `/api/v1/mosques` | SUPER_ADMIN |
| 12 | PUT | `/api/v1/mosques/{id}` | SUPER_ADMIN, MOSQUE_ADMIN |
| 13 | DELETE | `/api/v1/mosques/{id}` | SUPER_ADMIN |

Hit every endpoint per allowed role, plus no token (401) and a garbage token (401).

## 2. Step-by-step

**Step 1 — Auth basics:** no token / garbage token → 401 on GET `/mosques`.

**Step 2 — GET `/mosques` (pagination):** any role → 200 with pagination fields;
test `?page=0&size=2`. SUPER_ADMIN sees every mosque (incl. inactive);
PARENT/STUDENT also 200 (role-agnostic).

**Step 3 — GET `/mosques/{id}`:** SUPER_ADMIN → 200. A member of that mosque
(throwaway admin) → 200. Seed MOSQUE_ADMIN/TEACHER/STUDENT (no assignment) →
403 (`assertCanAccessMosque`). Invalid UUID → 400; random UUID → 404. A
deactivated mosque is still retrievable (no `is_active` filter on read).

**Step 4 — GET `/mosques/search` (TEACHER/STUDENT):** `q=`/`city=` optional —
missing q → 200 (all active). TEACHER/STUDENT → 200; MOSQUE_ADMIN/SUPER_ADMIN/
PARENT → 403. **Logic:** a user with a PENDING join request gets 403 on search
(`searchMosques` blocks while pending).

**Step 5 — GET `/mosques/member-join/preview` (TEACHER/STUDENT):** valid teacher
code + `role=TEACHER` → 200 (`mosqueName`); invalid/missing code → 404/400.
`role=PARENT` with any code → 404 (service only handles TEACHER/STUDENT codes).
MOSQUE_ADMIN → 403.

**Step 6 — POST `/mosques/join-requests` (TEACHER/STUDENT):** body
`{"mosqueId": "<uuid>"}` → 201. **Join-request flow:** member submits → admin
approves/rejects via `/api/v1/mosque-admins/join-requests/{id}` (see
mosque-admin-api guide); this guide tests submit + cancel:
submit again while pending → 403; pending blocks search → 403;
DELETE `/join-requests/my` → 200 (CANCELLED); cancel again → 404 (no pending).
Inactive mosque → 400 (`"Mosque is not active"`). MOSQUE_ADMIN/SUPER_ADMIN →
403 (role).

**Step 7 — GET `/mosques/invite-codes` (MOSQUE_ADMIN):** assigned admin → 200
(admin/teacher/student codes; missing ones generated on read). Seed MOSQUE_ADMIN
(no assignment) → 403; TEACHER/SUPER_ADMIN → 403.

**Step 8 — GET `/mosques/join/preview` (MOSQUE_ADMIN):** valid admin invite code
→ 200; invalid → 404; missing code → 400. TEACHER → 403.

**Step 9 — POST `/mosques/onboard` (MOSQUE_ADMIN):** body `MosqueCreateRequest`
(`name` required, `address`, `city`, `phone`, `email`, `logoUrl`, `timezone`) →
201 with `mosque`, `admin`, `inviteCode`, `teacherInviteCode`,
`studentInviteCode`. Re-onboard with an already-assigned admin → 403; blank name
→ 400; TEACHER/STUDENT/SUPER_ADMIN → 403. **No audit-reason header is required
for onboard** (SUPER_ADMIN is 403 on role anyway — onboard is not a
cross-tenant override).

**Step 10 — POST `/mosques/join` (MOSQUE_ADMIN):** body `{"inviteCode": "..."}`
→ 201 (creates a non-primary admin assignment). Invalid code → 404; blank code
→ 400; join with an assigned admin → 403; TEACHER → 403.

**Step 11 — POST `/mosques` (SUPER_ADMIN):** create → 201 (no admin assignment
created). MOSQUE_ADMIN/TEACHER/STUDENT → 403. Blank name → 400.

**Step 12 — PUT `/mosques/{id}`:** SUPER_ADMIN → 200; MOSQUE_ADMIN of that
mosque → 200; seed MOSQUE_ADMIN (no assignment) → 403; `name` > 200 chars →
400; random UUID → 404.

**Step 13 — DELETE `/mosques/{id}` (SUPER_ADMIN):** soft deactivate → 200
(`is_active=false`, row preserved). MOSQUE_ADMIN → 403; random UUID → 404.
**Note:** no audit-reason requirement on mosque delete (unlike mosque-admin
delete).

## 3. DB changes (DBeaver)

- `POST /onboard` → row in `mosques` (with `settings` jsonb containing
  `adminInviteCode`/`teacherInviteCode`/`studentInviteCode`) **and** a row in
  `mosque_admins` (permission `FULL_ACCESS`, `is_primary_admin=true`,
  `assigned_by` = the admin, `assigned_at` set).
- `POST /join` → new `mosque_admins` row (`is_primary_admin=false`).
- `POST /join-requests` → row in `mosque_join_requests`
  (`requested_role`, `status=PENDING`, `created_at`). Partial unique index
  `mosque_join_requests_one_pending_per_user` allows one pending row per user.
- `DELETE /join-requests/my` → `status=CANCELLED` on the pending row,
  `updated_at` bumped.
- `POST /mosques`, `PUT /mosques/{id}` → `mosques` row, `updated_at` bumped,
  `version` bumped, `created_at` untouched.
- `DELETE /mosques/{id}` → `is_active=false`, row present, `updated_at`/`version`
  bumped.
- `settings` merge on PUT preserves invite codes (they are not overwritable).

## 4. Flow — normal and logical

- Mosque admin onboarding: admin registers → GET `/join/preview` (confirm name)
  → POST `/onboard` → receives admin + teacher + student invite codes → shares
  teacher/student codes.
- Member join-request flow: teacher/student searches mosque → submits
  `/join-requests` → mosque admin approves (profile created) or rejects; member
  can cancel while pending. A user with a pending request is blocked from
  searching/submitting again (single pending per user).
- Direct join: teacher/student uses the role-specific invite code via
  `/teachers/join` or `/students/join` (see teacher-api/student-api guides).

## 5. Role UI (frontend)

- MOSQUE_ADMIN: mosque onboarding/join UI, invite-codes screen, approve/reject
  join requests.
- TEACHER/STUDENT: mosque search + join/request UI (preview confirm step).
- SUPER_ADMIN: full mosque list + create/edit/deactivate controls.
- Note: no student/teacher/parent self-serve CTA to *create* a mosque.

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. For
manual-only cases use:

| # | Endpoint | Role | Input | Expected | Actual | DB after | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | POST /mosques/onboard | MOSQUE_ADMIN | name=Riyadh Test | 201, codes returned | | mosques+mosque_admins rows | ✅/❌ |

## Known gaps to flag

- Search results are not limited to "other" mosques: `searchMosques` returns
  every active mosque, including one the caller already belongs to; it also
  returns an unpaginated `List` (large-DB concern).
- GET `/mosques/{id}` has no `is_active` filter: a deactivated mosque is still
  retrievable by id.
- `previewMemberJoin` with `role=PARENT` (or any non-member role) returns 404
  (`ResourceNotFoundException`) rather than a 400 — misclassified error.
- No explicit invite-code rotation endpoint; codes are only (re)generated
  lazily on `GET /invite-codes` when missing, and are preserved across settings
  updates.
- SUPER_ADMIN cannot onboard a mosque for another admin via `/onboard` (role
  restricted); a mosque without any admin can only be created via `POST /mosques`
  and then assigned via `/api/v1/mosque-admins`.
