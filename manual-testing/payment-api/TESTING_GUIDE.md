# Payment API — Testing Guide

Step-by-step manual test plan for **`/api/v1/payments`** (Payment Management:
student fee payments, billing cycles, receipts), mapped to the project's
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

- Backend running (port 8089) with the 5 seed users.
- **Domain data required for successful writes**: payments reference a
  **Student** (`studentId`), a **Circle** (`circleId`), a **Mosque**
  (`mosqueId`) and the **caller as recordedBy**; the caller must be able to
  access the mosque. The seed users have no profiles, so on a fresh DB most
  create cases cannot succeed — the script resolves IDs dynamically and marks
  cases `SKIPPED` when prerequisites are missing.
- Tokens per role: login as SUPER_ADMIN, MOSQUE_ADMIN, TEACHER, STUDENT, PARENT.
- DBeaver open on the `darb` DB (`payments` table).

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/payments` | SUPER_ADMIN, MOSQUE_ADMIN |
| 2 | GET | `/api/v1/payments/{id}` | any authenticated (mosque access enforced) |
| 3 | GET | `/api/v1/payments/mosque/{mosqueId}` | SUPER_ADMIN, MOSQUE_ADMIN |
| 4 | POST | `/api/v1/payments` | SUPER_ADMIN, MOSQUE_ADMIN |
| 5 | PUT | `/api/v1/payments/{id}` | SUPER_ADMIN, MOSQUE_ADMIN |

Enums:
- `PaymentStatus`: `PENDING`, `PARTIAL`, `PAID`, `OVERDUE`, `WAIVED`, `REFUNDED`.
- `PaymentMethod`: `CASH`, `BANK_TRANSFER`, `CARD`, `ONLINE`, `OTHER`.
- `PaymentCycle`: `MONTHLY`, `QUARTERLY`, `SEMESTER`, `ANNUAL`, `ONE_TIME`.

Hit every endpoint per allowed role, plus no token (401), garbage token (401),
invalid enum (400), and nonexistent id (404).

## 2. Step-by-step

**Step 1 — Auth basics:** no token → 401 on all 5. Garbage token → 401.

**Step 2 — GET `/payments` (list):** SUPER_ADMIN → 200 (all tenants);
MOSQUE_ADMIN → 200 (mosque-scoped, empty without assignment);
TEACHER/STUDENT/PARENT → 403.

**Step 3 — POST `/payments` (create):** SUPER_ADMIN with a real student + circle
+ mosque → 201 (DB checks: student → 404, circle → 404, mosque → 404,
`assertCanAccessMosque` → 403). MOSQUE_ADMIN **without** a mosque assignment →
403 (the mosque-access check runs on create). TEACHER/STUDENT/PARENT → 403.
Empty body → 400; `status="REFUNDED_X"` → 400. Random studentId → 404;
nonexistent mosque (real student+circle) → 404.

**Step 4 — GET `/payments/{id}`:** existing payment → SUPER_ADMIN 200; STUDENT
(no mosque) → 403. Random UUID → 404. Invalid UUID → 400.

**Step 5 — GET `/payments/mosque/{mosqueId}`:** real mosque → SUPER_ADMIN 200;
MOSQUE_ADMIN without assignment → 403. **Note:** there is **no mosque existence
check** — SUPER_ADMIN + random mosqueId → 200 empty page, not 404. Invalid UUID
→ 400.

**Step 6 — PUT `/payments/{id}`:** existing payment → SUPER_ADMIN 200;
MOSQUE_ADMIN needs mosque access (seed role without assignment → 403). TEACHER →
403. Invalid `status` enum → 400. Nonexistent id → 404.

## 3. DB changes (DBeaver)

- `POST /payments` → row in `payments` (`student_id`, `circle_id`, `mosque_id`,
  `amount`, `discount`, `amount_paid`, `status`, `method`, `cycle`, `due_date`,
  `paid_date`, `receipt_url`, `recorded_by`, `notes`).
- `PUT /payments/{id}` → `discount` / `amount_paid` / `status` / `method` /
  `due_date` / `paid_date` / `receipt_url` / `notes` updated.
- There is **no DELETE endpoint** (controller has none; `PaymentService.delete`
  is not exposed).

## 4. Flow — normal and logical

- Happy path: MOSQUE_ADMIN/SUPER_ADMIN creates a PENDING monthly invoice →
  student pays → status flips PENDING → PARTIAL → PAID (`amountPaid` tracked) →
  receipt URL attached. Overdue handling is stored (`OVERDUE`), not auto-computed.
- Logic checks worth re-testing by hand: mosque-access enforcement on create/
  update (`assertCanAccessMosque`), partial-payment status transitions, and the
  missing mosque-existence check on `GET /payments/mosque/{mosqueId}`.

## 5. Role UI (frontend)

- MOSQUE_ADMIN/SUPER_ADMIN: invoice/collection screens, status + receipt
  management.
- STUDENT/PARENT: view own/child fees (student-scoped APIs; the payments API
  itself is admin-only for lists, with per-record access checks).
- TEACHER: no payment screens (illegal CTAs absent).

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. For
manual-only cases use:

| # | Endpoint | Role | Input | Expected | Actual | DB after | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | POST /payments | SUPER_ADMIN | student + circle + mosque | 201 | | | ✅/❌ |
| 2 | PUT /payments/{id} | MOSQUE_ADMIN | status=PAID | 200 | | | ✅/❌ |

## Known gaps to flag

- (a) `status`, `method` and `cycle` are `NOT NULL` in the DB but optional in
  `PaymentCreateRequest` — omitting them causes a DB error (500), not a 400.
- (b) `GET /payments/mosque/{mosqueId}` performs **no mosque existence check**
  — a nonexistent mosque returns 200 empty (SUPER_ADMIN), contradicting the
  documented 404.
- (c) No DELETE mapping on the controller, though `PaymentService.delete`
  exists.
