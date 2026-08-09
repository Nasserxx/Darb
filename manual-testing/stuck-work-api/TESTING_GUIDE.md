# Stuck Work API — Testing Guide

Step-by-step manual test plan for **`/api/v1/admin/stuck-work`** (SUPER_ADMIN
cross-mosque control plane), mapped to the project's testing considerations:

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
- Tokens: SUPER_ADMIN (positive) + MOSQUE_ADMIN/TEACHER/STUDENT/PARENT
  (negative 403).
- DBeaver open on the `darb` DB.
- Seed data has **no pending join requests**, so the list is empty by default.
  `run-tests.ps1` (non-`-ReadOnly`) creates a throwaway mosque + a throwaway
  STUDENT and submits a pending join request so the list has real data.

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/admin/stuck-work` | SUPER_ADMIN only (`@PreAuthorize("hasRole('SUPER_ADMIN')")`) |

No path/query parameters → no 400/404 input cases.

## 2. Step-by-step

**Step 1 — auth basics:** no token → **401**. Garbage token → **401**.

**Step 2 — roles:** SUPER_ADMIN → **200** with `data` = list of
`StuckWorkItem` (`kind`, `mosqueId`, `mosqueName`, `resourceId`, `summary`,
`createdAt`, `densityScore`). MOSQUE_ADMIN/TEACHER/STUDENT/PARENT → **403**
(even though MOSQUE_ADMIN is a "admin", the fleet hub is SUPER_ADMIN-only).

**Step 3 — with data (logic):** with at least one PENDING
`mosque_join_requests` row, SUPER_ADMIN's list shows a `PENDING_JOIN` item:
`summary` = "<name> requested to join as <role>", sorted by `densityScore`
descending then `createdAt` ascending (density = count of pending requests in
that mosque).

## 3. DB changes (DBeaver)

- **No writes** — read-only (`@Transactional(readOnly = true)`).
- Read source table: `mosque_join_requests` (`status = 'PENDING'`). Each item
  maps from the join request; `densityScore` is the count of PENDING requests
  per mosque.
- To prepare manual data: register a throwaway STUDENT, create a mosque
  (`POST /api/v1/mosques` as SUPER_ADMIN), then `POST /api/v1/mosques/join-requests`
  `{ "mosqueId": ... }` as the throwaway STUDENT → a PENDING row appears and the
  fleet list shows it. Approving/rejecting via the mosque-admin APIs changes
  the status and removes it from this list.

## 4. Flow — normal and logical

- SUPER_ADMIN fleet hub: on load, GET `/admin/stuck-work` → shows pending
  join-request work across all mosques, most crowded first. Empty list = no
  backlog.
- The script's wiring: throwaway mosque + throwaway student join request →
  list contains ≥ 1 item; the item's `resourceId` is the join request id, and
  `kind` is `PENDING_JOIN`.

## 5. Role UI (frontend)

- SUPER_ADMIN: fleet hub "stuck work" queue.
- Other roles: **no entry point** (illegal CTA absent). Even MOSQUE_ADMIN has
  no access to this fleet-level view — they manage their own pending requests
  via `/api/v1/mosque-admins/join-requests`.

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. For
manual-only cases use:

| # | Endpoint | Role | Input | Expected | Actual | DB after | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | GET /admin/stuck-work | SUPER_ADMIN | 1 pending join | 200, ≥1 item | | | ✅/❌ |

## Known gaps to flag

- None observed on the endpoint itself (auth scoping is correct: SUPER_ADMIN
  only). Seed data simply has no pending work, so data-driven cases need the
  throwaway wiring shown above.
