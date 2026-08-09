# Mosque Admin API — Testing Guide

Step-by-step manual test plan for **`/api/v1/mosque-admins`** (mosque admin
assignments, permissions, join-request approval), mapped to the project's
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

- Backend running (port 8089) with the 5 seed users. The seeded MOSQUE_ADMIN has
  **no mosque assignment** — use throwaway admins for the assignment-based cases.
- Throwaway users: the script registers a fresh cohort (`ma.*@darb.app`,
  timestamp-suffixed): `ma.admin` onboards a throwaway mosque, `ma.admin2` joins
  it, `ma.admin3` is the target of SUPER_ADMIN create/update/delete, and
  `ma.teacher`/`ma.student` drive the join-request approve/reject flow.
- Tokens per role: login as SUPER_ADMIN, MOSQUE_ADMIN, TEACHER, STUDENT, PARENT.
- DBeaver open on the `darb` DB.

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/mosque-admins/join-requests` | MOSQUE_ADMIN |
| 2 | POST | `/api/v1/mosque-admins/join-requests/{id}/approve` | MOSQUE_ADMIN |
| 3 | POST | `/api/v1/mosque-admins/join-requests/{id}/reject` | MOSQUE_ADMIN |
| 4 | GET | `/api/v1/mosque-admins` | SUPER_ADMIN, MOSQUE_ADMIN |
| 5 | GET | `/api/v1/mosque-admins/{id}` | SUPER_ADMIN, MOSQUE_ADMIN |
| 6 | POST | `/api/v1/mosque-admins` | SUPER_ADMIN |
| 7 | PUT | `/api/v1/mosque-admins/{id}` | SUPER_ADMIN |
| 8 | DELETE | `/api/v1/mosque-admins/{id}` | SUPER_ADMIN |

Hit every endpoint per allowed role, plus no token (401) and a garbage token (401).

### SUPER_ADMIN override audit — header vs body (verified in `OverrideAuditService`)

`OverrideAuditService.gateSuperAdmin` forces an audit reason (min **8 chars**) on
every SUPER_ADMIN write to this resource. It is read from **both** the
`X-Audit-Reason` header and the request body `auditReason` field:

- `POST` / `PUT` → body `auditReason` works (controller passes it through).
- `DELETE` → the controller passes `null` for the body reason, so the **header
  is mandatory**: `X-Audit-Reason: <reason >= 8 chars>`.
- Missing/short reason → **400**.

## 2. Step-by-step

**Step 1 — Auth basics:** no token / garbage token → 401 on GET `/mosque-admins`.

**Step 2 — GET `/mosque-admins/join-requests` (MOSQUE_ADMIN):** assigned admin
→ 200 (list of pending requests for that mosque). Seed MOSQUE_ADMIN (no
assignment) → 403 `"No mosque assignment found"`. TEACHER → 403.

**Step 3 — POST `.../join-requests/{id}/approve` (MOSQUE_ADMIN):** with a
pending TEACHER request (created via `POST /mosques/join-requests`):
approve → 200 (creates the teacher profile), approve again → 400 `"Join request
is not pending"`. Random UUID → 404; invalid UUID → 400; approve by seed
MOSQUE_ADMIN (no assignment) → 403; TEACHER → 403.

**Step 4 — POST `.../join-requests/{id}/reject` (MOSQUE_ADMIN):** with a pending
STUDENT request: reject → 200 (no profile created), reject again → 400.
Random UUID → 404; invalid UUID → 400; seed MOSQUE_ADMIN (no assignment) → 403;
TEACHER → 403.

**Step 5 — GET `/mosque-admins` (pagination):** SUPER_ADMIN → 200 (all
assignments). Assigned MOSQUE_ADMIN → 200 (scoped to its mosque only). Seed
MOSQUE_ADMIN (no assignment) → 200 **empty** page (`pageForCaller`).
TEACHER/STUDENT/PARENT → 403.

**Step 6 — GET `/mosque-admins/{id}`:** SUPER_ADMIN → 200 (any row). Assigned
MOSQUE_ADMIN → 200 (same mosque); different mosque → 403. Seed MOSQUE_ADMIN (no
assignment) → 403. TEACHER → 403. Invalid UUID → 400; random UUID → 404.

**Step 7 — POST `/mosque-admins` (SUPER_ADMIN):** body `MosqueAdminCreateRequest`
(`userId`, `mosqueId`, `permission` [FULL_ACCESS, MANAGE_TEACHERS,
MANAGE_STUDENTS, MANAGE_CIRCLES, MANAGE_PAYMENTS, VIEW_REPORTS],
`isPrimaryAdmin`, optional `auditReason`) → **201 with `X-Audit-Reason` or body
`auditReason` (>=8 chars)**. Without audit reason → 400. MOSQUE_ADMIN → 403.
Nonexistent `userId` → 404. Note: `uq_mosque_admins_user_mosque` rejects a
duplicate (user, mosque) pair.

**Step 8 — PUT `/mosque-admins/{id}` (SUPER_ADMIN):** update `permission` /
`isPrimaryAdmin` → 200 with audit reason; without audit reason → 400.
MOSQUE_ADMIN → 403. Random UUID → 404.

**Step 9 — DELETE `/mosque-admins/{id}` (SUPER_ADMIN):** remove → 200 **with
`X-Audit-Reason` header** (body reason is ignored on delete). Without header →
400. MOSQUE_ADMIN → 403. Random UUID → 404.

## 3. DB changes (DBeaver)

- `POST /mosque-admins` → row in `mosque_admins` (`permission`,
  `is_primary_admin`, `assigned_at`, `assigned_by` = SUPER_ADMIN) **and** a row
  in `override_audit_log` (`action=MOSQUE_ADMIN_ASSIGN`, `reason`, actor =
  SUPER_ADMIN, `resource_type=MosqueAdmin`, `resource_id`).
- `PUT /mosque-admins/{id}` → `permission`/`is_primary_admin` updated,
  `updated_at`/`version` bumped; `override_audit_log` row
  (`action=MOSQUE_ADMIN_UPDATE`).
- `DELETE /mosque-admins/{id}` → row **hard-deleted** from `mosque_admins`;
  `override_audit_log` row (`action=MOSQUE_ADMIN_REMOVE`).
- `approve` join request → new `teachers` or `students` row (profile created) +
  `mosque_join_requests.status=APPROVED`, `reviewed_at`, `reviewed_by` set.
- `reject` join request → `mosque_join_requests.status=REJECTED`, `reviewed_at`,
  `reviewed_by` set; no profile row.

## 4. Flow — normal and logical

- Mosque admin onboarding: `ma.admin` onboards mosque → shares admin invite code
  → `ma.admin2` joins via `POST /mosques/join` → both now appear in
  `GET /mosque-admins`.
- Join-request approval: teacher submits `POST /mosques/join-requests` → admin
  lists pending (`GET /mosque-admins/join-requests`) → approve (profile created)
  or reject. Approving/rejecting twice or on the wrong mosque → 400/403.
- SUPER_ADMIN override: assign / update / remove an admin — every write must
  carry an audit reason or it is rejected with 400 and **no** DB change.

## 5. Role UI (frontend)

- MOSQUE_ADMIN: "join requests" screen (approve/reject), member management.
- SUPER_ADMIN: admin-assignment management UI (assign, change permission,
  remove) — must collect an audit reason before submitting.
- TEACHER/STUDENT/PARENT: no entry points to this resource (illegal CTAs absent).

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. For
manual-only cases use:

| # | Endpoint | Role | Input | Expected | Actual | DB after | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | POST /mosque-admins | SUPER_ADMIN | userId+mosqueId+auditReason | 201 | | mosque_admins + override_audit_log rows | ✅/❌ |

## Known gaps to flag

- `DELETE /mosque-admins/{id}` ignores the body `auditReason` (controller passes
  `null`) — the `X-Audit-Reason` **header is mandatory**, which is inconsistent
  with create/update (header or body both accepted).
- No guard against removing the last (primary) admin or demoting the only admin:
  a mosque can end up with **zero** `mosque_admins` rows and become unmanageable.
- `MosqueAdminService.create` does not validate that the target user's role is
  MOSQUE_ADMIN (any role can be assigned as a mosque admin) or check
  `is_active`; a duplicate (user, mosque) assignment surfaces as a DB unique
  constraint failure rather than a friendly 400.
- `pageForCaller` returns an empty page (200) for a MOSQUE_ADMIN without an
  assignment instead of an error — acceptable but easy to misinterpret as "no
  data" when it really means "no assignment".
