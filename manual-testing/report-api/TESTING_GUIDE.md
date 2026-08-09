# Report API — Testing Guide

Step-by-step manual test plan for **`/api/v1/reports`** (analytical reports
for mosques), mapped to the project's testing considerations:

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
- Tokens: SUPER_ADMIN + MOSQUE_ADMIN (allowed), TEACHER/STUDENT/PARENT
  (forbidden 403).
- DBeaver open on the `darb` DB.
- Seed data has **no mosques and no reports**, so `run-tests.ps1` (non-`-ReadOnly`)
  creates a **throwaway mosque** (`POST /api/v1/mosques` as SUPER_ADMIN) and
  **throwaway reports**; destructive/nonexistent-id cases use these + random
  UUIDs. Seed data is never touched.

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/reports/{id}` | SUPER_ADMIN, MOSQUE_ADMIN (`@PreAuthorize`) + tenant check (`assertCanAccessMosque`) |
| 2 | GET | `/api/v1/reports/mosque/{mosqueId}` | SUPER_ADMIN, MOSQUE_ADMIN (`@PreAuthorize`) |
| 3 | POST | `/api/v1/reports` | SUPER_ADMIN, MOSQUE_ADMIN (`@PreAuthorize`) |

Note: `ReportService.update/delete` exist (`ReportService.java:71,88`) but the
controller exposes **no** PUT/DELETE endpoint — reports cannot be modified or
deleted through the API (see gaps).

## 2. Step-by-step

**Step 1 — auth basics:** no token → **401** on all three endpoints.

**Step 2 — GET `/reports/{id}`:** SUPER_ADMIN on an existing report → **200**
(`id`, `mosqueId`, `generatedBy`, `type`, `title`, `filters`, `fileUrl`,
`generatedAt`). MOSQUE_ADMIN with **no mosque assignment** → **403**
(`findById` calls `assertCanAccessMosque`; a report only resolves 200 for a
mosque admin assigned to that report's mosque). TEACHER/STUDENT/PARENT →
**403**. Invalid UUID → **400**. Random UUID → **404**.

**Step 3 — GET `/reports/mosque/{mosqueId}`:** SUPER_ADMIN → **200**,
paginated (`@PageableDefault(size = 20)`; fields `content`, `pageNumber`,
`pageSize`, `totalElements`, `totalPages`, `last`). MOSQUE_ADMIN → **200** for
**any** mosque id (this service method does **not** call
`assertCanAccessMosque` — gap (b)). TEACHER/STUDENT/PARENT → **403**. Invalid
UUID → **400**. Random mosque UUID → **200 with empty page** (no mosque
existence check — gap (c); OpenAPI documents 404 but it is not enforced).
Test `?page=0&size=1`.

**Step 4 — POST `/reports`:** body `{ mosqueId, type, title, filters?, fileUrl? }`
(`generatedBy` is set server-side from the token). SUPER_ADMIN → **201**.
MOSQUE_ADMIN → **201** for a mosque they are not assigned to (no tenant check —
gap (b)). TEACHER/STUDENT/PARENT → **403**. Missing `title` / missing `type` /
invalid enum → **400**. Random `mosqueId` → **404**.

## 3. DB changes (DBeaver)

- Table `reports` (`mosque_id`, `generated_by`, `type`, `title`, `filters`
  jsonb, `file_url`, `generated_at`, plus `created_at`/`updated_at`/`version`
  from `BaseAuditableEntity`).
- `POST /reports` → one `reports` row: `generated_at = now`,
  `created_at = updated_at = now`, `version = 0`, `type`/`title` as sent,
  `filters`/`file_url` stored when provided.
- No update/delete writes are possible via the API (no endpoints). Manual SQL
  could verify `version` bumps on a direct update.
- `override_audit_log` is **not** written by `ReportService` (SUPER_ADMIN
  report writes need no audit reason).

## 4. Flow — normal and logical

- Happy path: create mosque → create report (SUPER_ADMIN) → GET by id → GET by
  mosque (paginated). MOSQUE_ADMIN of the same mosque sees the report; a
  MOSQUE_ADMIN from another mosque gets 403 on `{id}` but still 200 on the
  mosque listing (gap).
- Nonexistent report → 404 on `{id}`; nonexistent mosque → empty page on the
  list.

## 5. Role UI (frontend)

- SUPER_ADMIN / MOSQUE_ADMIN: report generation form + report list/read views.
- TEACHER/STUDENT/PARENT: no report CTAs at all (illegal CTAs absent, not
  greyed).

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. For
manual-only cases use:

| # | Endpoint | Role | Input | Expected | Actual | DB after | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | GET /reports/{id} | MOSQUE_ADMIN (other mosque) | existing report | 403 | | | ✅/❌ |

## Known gaps to flag

- (a) **No PUT/DELETE endpoints.** `ReportService.update/delete` are unused;
  reports can be created and read but never edited or removed via the API.
- (b) **Missing tenant isolation** on `GET /reports/mosque/{mosqueId}` and
  `POST /reports` — a MOSQUE_ADMIN can list/create reports for any mosque
  without an assignment (compare `GET /reports/{id}` which does enforce it).
- (c) `GET /reports/mosque/{mosqueId}` does not validate that the mosque
  exists — random mosque id returns 200 with an empty page (OpenAPI documents
  404).
