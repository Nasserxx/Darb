# Memorization Progress API — Testing Guide

Step-by-step manual test plan for **`/api/v1/memorization`** (Memorization
Progress: Quran memorization tracking, recitation grades, tajweed scores),
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
- **Domain data required for successful writes**: progress records reference a
  **Student** (`studentId`), **Circle** (`circleId`), **Teacher** (`teacherId`)
  and require an **ACTIVE enrollment** for the student+circle pair. The seed
  users have no profiles, so on a fresh DB most create cases cannot succeed —
  the script resolves IDs dynamically and marks cases `SKIPPED` when
  prerequisites are missing.
- Tokens per role: login as SUPER_ADMIN, MOSQUE_ADMIN, TEACHER, STUDENT, PARENT.
- DBeaver open on the `darb` DB (`memorization_progress` table).

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/memorization/{id}` | any authenticated (student access enforced) |
| 2 | GET | `/api/v1/memorization/student/{studentId}` | any authenticated (student access enforced) |
| 3 | GET | `/api/v1/memorization/circle/{circleId}` | any authenticated (mosque access enforced) |
| 4 | POST | `/api/v1/memorization` | SUPER_ADMIN, MOSQUE_ADMIN, TEACHER |
| 5 | PUT | `/api/v1/memorization/{id}` | SUPER_ADMIN, MOSQUE_ADMIN, TEACHER |
| 6 | POST | `/api/v1/memorization/me` | STUDENT (self-report, own profile) |

**Domain facts (verify):** `POST /me` is the **student self-record** endpoint —
the controller resolves the student profile from the JWT (`findByUserId`) and
requires an **ACTIVE enrollment** in the target circle before recording.

Enums:
- `RecitationGrade`: `EXCELLENT`, `VERY_GOOD`, `GOOD`, `ACCEPTABLE`, `POOR`.

Hit every endpoint per allowed role, plus no token (401), garbage token (401),
invalid enum (400), and nonexistent id (404).

## 2. Step-by-step

**Step 1 — Auth basics:** no token → 401 on all 6. Garbage token → 401.

**Step 2 — POST `/memorization` (record progress):** SUPER_ADMIN with an ACTIVE
enrollment (student+circle) and a real teacher → 201 (DB checks: student →
404, circle → 404, teacher → 404, no enrollment for the pair → 400, enrollment
not ACTIVE → 400). The controller asserts access to the student first, so
MOSQUE_ADMIN/TEACHER **without** a mosque assignment → 403. STUDENT/PARENT →
403. Empty body → 400; `grade="AVERAGE"` → 400. Random studentId → 404.

**Step 3 — GET `/memorization/{id}`:** existing record → SUPER_ADMIN 200, PARENT
without link → 403. Random UUID → 404. Invalid UUID → 400.

**Step 4 — GET `/memorization/student/{studentId}`:** real student → SUPER_ADMIN
200, PARENT without link → 403. Random UUID → 404 (student lookup in access
assert). Invalid UUID → 400.

**Step 5 — GET `/memorization/circle/{circleId}`:** real circle → SUPER_ADMIN
200; STUDENT without mosque → 403. Random UUID → 404 (circle lookup). Invalid
UUID → 400.

**Step 6 — POST `/memorization/me` (STUDENT self-report):** seed student has no
profile → 404 (`Student not found for userId`). Once the student has a profile
+ ACTIVE enrollment in the circle → 201. With a profile but **no** ACTIVE
enrollment → 400 `"No active enrollment found for this circle"`. TEACHER →
403 (`@PreAuthorize hasRole STUDENT`).

**Step 7 — PUT `/memorization/{id}`:** existing record → SUPER_ADMIN 200;
TEACHER/MOSQUE_ADMIN need mosque access (seed roles → 403). STUDENT → 403.
Invalid `grade` enum → 400. Nonexistent id → 404.

## 3. DB changes (DBeaver)

- `POST /memorization` / `POST /memorization/me` → row in
  `memorization_progress` (`student_id`, `circle_id`, `teacher_id`,
  `surah_number`, `ayah_from`, `ayah_to`, `grade`, `tajweed_score`,
  `teacher_notes`, `session_date`).
- `PUT /memorization/{id}` → `grade` / `tajweed_score` / `teacher_notes` /
  `audio_url` updated.
- There is **no list-all endpoint** (controller has no `GET /` mapping even
  though `MemorizationProgressService.findAll` exists) and **no DELETE**
  endpoint — progress records cannot be listed globally or removed via API.

## 4. Flow — normal and logical

- Happy path: TEACHER records progress (grade + tajweed) for a student in the
  circle → student sees it via `GET /memorization/student/{studentId}` →
  student self-reports an extra surah via `POST /me` (own circle) → teacher
  adjusts the grade via `PUT /{id}`.
- Logic checks worth re-testing by hand: student+circle with no enrollment
  (400), non-ACTIVE enrollment (400), unlinked PARENT read (403), self-report
  without profile (404).

## 5. Role UI (frontend)

- TEACHER: grade/record recitation in the circle session sheet.
- STUDENT: self-report progress; view own progress history.
- PARENT: view child progress (student-scoped API).
- SUPER_ADMIN/MOSQUE_ADMIN: review/grading screens (MOSQUE_ADMIN tenant-scoped).

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. For
manual-only cases (needs an ACTIVE enrollment / student profile) use:

| # | Endpoint | Role | Input | Expected | Actual | DB after | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | POST /memorization | SUPER_ADMIN | ACTIVE enrollment + teacher | 201 | | | ✅/❌ |
| 2 | POST /memorization/me | STUDENT | own circle, ACTIVE enrollment | 201 | | | ✅/❌ |

## Known gaps to flag

- (a) `grade` is `NOT NULL` in the DB but optional in
  `MemorizationProgressCreateRequest` — omitting it causes a DB error (500),
  not a 400.
- (b) No `GET /` (list-all) and no `DELETE` mapping on the controller, even
  though the service implements both.
- (c) `POST /memorization` for MOSQUE_ADMIN/TEACHER is gated by the controller's
  `assertCanAccessStudent`; without a mosque assignment it is effectively
  unusable for those roles (403) until profiles are wired.
