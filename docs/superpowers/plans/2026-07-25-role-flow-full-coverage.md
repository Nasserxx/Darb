# Role-Flow Full Coverage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close live role-flow gaps so Parent invite → Admin link → Teacher mark → Parent see → Parent excuse → Teacher see works end-to-end, with SUPER_ADMIN as whole-system controller (stuck-work hub + audited overrides).

**Architecture:** PARENT stays link-scoped (never `mosque_id` / never `pageForCaller`). Every non-SA `findById` asserts via `MosqueAccessService`. Parent UX reads student-scoped enrollments/attendance. SA control plane is a fleet stuck-work hub with mandatory audit reason on cross-tenant overrides. Illegal CTAs are absent, not greyed.

**Tech Stack:** Spring Boot 3 / JPA / Flyway (backend), React + TanStack Query + i18n (frontend), MockMvc Postgres integration tests (`PostgresIntegrationTestBase`).

**Spec:** `docs/superpowers/specs/2026-07-25-role-flow-full-coverage-design.md`

## Global Constraints

- **NO GIT OPERATIONS.** Do not `git add`, `git commit`, `git push`, `git checkout`, reset, or amend. Skip every plan “Commit” step.
- PARENT is link-scoped only — never assign `mosque_id` to PARENT; never route parent lists through `assertCanAccessMosque` / `pageForCaller`.
- Attendance verbs: Mark = TEACHER (assigned circle) / MOSQUE_ADMIN / SUPER_ADMIN; Excuse = PARENT (linked child). PARENT UI must never expose Mark.
- Every non-SUPER_ADMIN `findById` must assert; resource UUID is not a capability.
- Dual invites: mosque join code ≠ `parentInviteCode`; labeled distinctly.
- Half-shipped CTAs forbidden: if API exists, wire FE; else hide the button.
- Names at handoff surfaces; UUIDs are wire IDs only.
- No impersonation sessions; SA stuck-path is read-clone + repair rails with audit.
- Out of scope: WebSockets, bulk mark, `actingAs` dual-hat, re-fixing stale list-leak P0s.
- Hard pause after Phase P1 (Tasks 1–6) for authz verification before P1b+.
- Prefer existing patterns in `TenantAccessIntegrationTest`, `MosqueAccessService.assertCanAccessStudent`.
- PowerShell: do not chain with `&&`; use `;` or separate Shell calls. Before exploring code: `graphify query "..."`.
- After modifying code files: `graphify update .` (AST-only).

## File map (create / modify)

| Area | Files |
|------|--------|
| Access helpers | `backend/.../security/MosqueAccessService.java` |
| IDOR guards | `AttendanceService`, `EnrollmentService`, `ParentStudentService` + matching controllers |
| Other findById audit | `GoalService`, `AchievementService`, `ReportService`, `MessageService`, `NotificationService`, `UserService` (+ controllers) |
| Parent FE API | `frontend/.../parent-students/api/parent-students-api.ts` |
| Parent circles | `frontend/.../circles/parent-circles-view.tsx`, `circles-page.tsx` |
| Parent attendance | `frontend/.../attendance/attendance-page.tsx` (+ optional `parent-attendance-view.tsx`) |
| Role affordances | `frontend/.../lib/navigation/app-nav.ts`, `role-permissions.ts`, shared route pages |
| Excuse | `AttendanceService.submitExcuse`, attendance FE API/hooks, circle attendance UI |
| SA hub | new DTOs/services/controllers under `backend/.../stuckwork/` or `admin/`, FE admin dashboard |
| Audit | new entity/migration `override_audit_log` (or reuse if present) |
| Invites | Student `parentInviteCode` rotate; mosque invite rotate UX |
| Tests | `backend/src/test/java/com/darb/controllers/v1/*IntegrationTest.java` |

---

## Phase P1 — Security + parent unblock

### Task 1: Guard Attendance / Enrollment / ParentStudent `findById` (IDOR)

**Files:**
- Modify: `backend/src/main/java/com/darb/security/MosqueAccessService.java`
- Modify: `backend/src/main/java/com/darb/services/AttendanceService.java`
- Modify: `backend/src/main/java/com/darb/services/EnrollmentService.java`
- Modify: `backend/src/main/java/com/darb/services/ParentStudentService.java`
- Modify: `backend/src/main/java/com/darb/controllers/v1/AttendanceController.java`
- Modify: `backend/src/main/java/com/darb/controllers/v1/EnrollmentController.java`
- Modify: `backend/src/main/java/com/darb/controllers/v1/ParentStudentController.java`
- Test: `backend/src/test/java/com/darb/controllers/v1/FindByIdAccessIntegrationTest.java` (create)

**Interfaces:**
- Consumes: `MosqueAccessService.assertCanAccessStudent(UUID, UUID)`, `assertCanAccessMosque`
- Produces: `findById(UUID callerId, UUID id)` on the three services; controllers pass `(UUID) authentication.getPrincipal()`

- [ ] **Step 1: Write failing integration tests**

Create `FindByIdAccessIntegrationTest` extending `PostgresIntegrationTestBase`, mirror helpers from `TenantAccessIntegrationTest` (register/login/onboard/createStudent). Cover at least:

```java
@Test
void mosqueAdmin_cannotGetAttendanceFromOtherMosque() throws Exception { /* setup A/B mosques, create attendance in B, GET /api/v1/attendance/{id} with token A → 403 */ }

@Test
void mosqueAdmin_cannotGetEnrollmentFromOtherMosque() throws Exception { /* same pattern for /api/v1/enrollments/{id} */ }

@Test
void parent_cannotGetForeignParentStudentLink() throws Exception { /* PARENT P1 linked to S1; GET link id for unrelated parent → 403 */ }

@Test
void parent_canGetOwnParentStudentLink() throws Exception { /* 200 */ }
```

- [ ] **Step 2: Run tests — expect FAIL (200 instead of 403)**

Run: `./mvnw -pl backend test -Dtest=FindByIdAccessIntegrationTest`  
(Windows: `.\mvnw.cmd -pl backend test "-Dtest=FindByIdAccessIntegrationTest"` from repo root, or `cd backend` then `.\mvnw.cmd test "-Dtest=FindByIdAccessIntegrationTest"`)

Expected: FAIL — foreign UUID returns 200.

- [ ] **Step 3: Add access helpers + wire findById**

In `MosqueAccessService`, add:

```java
public void assertCanAccessParentStudent(UUID callerId, ParentStudent link) {
    UserRole role = findUserRole(callerId);
    if (role == UserRole.SUPER_ADMIN) {
        return;
    }
    if (role == UserRole.PARENT) {
        if (!link.getParent().getId().equals(callerId)) {
            throw new ForbiddenException("Access denied to this parent-student link");
        }
        return;
    }
    if (role == UserRole.MOSQUE_ADMIN || role == UserRole.TEACHER) {
        assertCanAccessMosque(callerId, link.getStudent().getMosque().getId());
        return;
    }
    throw new ForbiddenException("Access denied to this parent-student link");
}
```

Change service signatures:

```java
// AttendanceService
public AttendanceResponse findById(UUID callerId, UUID id) {
    Attendance attendance = findEntityOrThrow(id);
    mosqueAccessService.assertCanAccessStudent(callerId, attendance.getEnrollment().getStudent().getId());
    return toResponse(attendance);
}

// EnrollmentService
public EnrollmentResponse findById(UUID callerId, UUID id) {
    Enrollment enrollment = findEntityOrThrow(id);
    mosqueAccessService.assertCanAccessStudent(callerId, enrollment.getStudent().getId());
    return toResponse(enrollment);
}

// ParentStudentService
public ParentStudentResponse findById(UUID callerId, UUID id) {
    ParentStudent link = findEntityOrThrow(id);
    mosqueAccessService.assertCanAccessParentStudent(callerId, link);
    return toResponse(link);
}
```

Controllers: pass caller principal into `findById` (same pattern as `StudentController.findById`).

Also guard `AttendanceService.delete` and `EnrollmentService`/`ParentStudentService` mutate-by-id if they currently skip asserts (fail closed).

- [ ] **Step 4: Re-run tests — expect PASS**

- [ ] **Step 5: Skip commit** (global constraint)

---

### Task 2: Audit remaining public `findById` surfaces

**Files:**
- Modify: `GoalService`, `AchievementService`, `ReportService`, `MessageService`, `NotificationService` (and controllers) — assert mosque/student/participant as appropriate
- `UserService.findById`: either restrict to self + SA/admin patterns already used, or document intentional openness; prefer assert for non-SA reading another user’s full profile if currently unscoped
- Test: extend `FindByIdAccessIntegrationTest` with one foreign-mosque case per newly guarded resource that is mosque-scoped

**Interfaces:**
- Consumes: Task 1 helpers
- Produces: all remaining mosque-scoped `findById(callerId, id)` fail closed

- [ ] **Step 1: Inventory** — for each unguarded `findById` in services listed above, note mosque/student path from entity
- [ ] **Step 2: Write one failing test per resource type that is mosque-tenant scoped**
- [ ] **Step 3: Implement asserts** (SUPER_ADMIN bypass; else mosque or participant assert)
- [ ] **Step 4: Tests PASS**
- [ ] **Step 5: Skip commit**

Acceptance: foreign UUID → 403/404 for non-SA; SA still 200.

---

### Task 3: Wire FE `getMyChildren`

**Files:**
- Modify: `frontend/src/features/parent-students/api/parent-students-api.ts`
- Verify: `frontend/src/features/parent-students/hooks/use-parent-students.ts` (`useMyChildren`)
- Verify: `frontend/src/features/parent-students/components/parent-children-view.tsx`

**Interfaces:**
- Consumes: `GET /api/v1/parent-students/my-children` → `List<StudentResponse>`
- Produces: `parentStudentsApi.getMyChildren(): Promise<StudentResponse[]>`

- [ ] **Step 1: Confirm hook calls missing method** — `useMyChildren` already calls `parentStudentsApi.getMyChildren()`; API object lacks it (compile/runtime break).

- [ ] **Step 2: Add API method**

```typescript
import type { StudentResponse } from "@/features/students/types/index.ts"; // or existing student type path

// inside parentStudentsApi:
getMyChildren: () => fetchData<StudentResponse[]>(`${BASE_PATH}/my-children`),
```

Use the same `StudentResponse` type the students feature already exports. If `fetchData` expects wrapped pages only, match how other list-of-T endpoints unwrap (inspect `fetchData` in `frontend/src/lib/api/pagination.ts`).

- [ ] **Step 3: Typecheck** — `cd frontend; npm run typecheck` (or project’s equivalent). Expect pass; `ParentChildrenView` shows child names.

- [ ] **Step 4: Skip commit**

---

### Task 4: Parent circles via enrollments (no `pageForCaller`)

**Files:**
- Modify: `frontend/src/pages/app/circles/parent-circles-view.tsx`
- Modify: `frontend/src/pages/app/circles/circles-page.tsx` (pass children from `useMyChildren` if needed)
- Optional BE: none if `findByStudentId` enrollments already assert via `assertCanAccessStudent`

**Interfaces:**
- Consumes: `useMyChildren`, `useStudentEnrollments(studentId)`, circle `getById` or enrollment payloads that include circle names when available
- Produces: parent circles list = ACTIVE enrollments’ circles for **all** linked children (no silent 3-child cap); never calls `useCircles` mosque page for PARENT

- [ ] **Step 1: Replace hard-coded child1/2/3 hooks**

Remove:

```typescript
const child1 = useChildEnrollments(parentStudentIds[0]);
const child2 = useChildEnrollments(parentStudentIds[1]);
const child3 = useChildEnrollments(parentStudentIds[2]);
```

Replace with a pattern that loads enrollments for every child id (map over ids; if React hooks rules block dynamic hooks, fetch via a small helper hook that takes `string[]` and uses `useQueries`).

Example:

```typescript
import { useQueries } from "@tanstack/react-query";
import { enrollmentsApi } from "@/features/enrollments/api/enrollments-api.ts";
import { enrollmentKeys } from "@/features/enrollments/hooks/query-keys.ts";

const enrollmentQueries = useQueries({
  queries: parentStudentIds.map((studentId) => ({
    queryKey: enrollmentKeys.byStudent(studentId, { page: 0, size: 500 }),
    queryFn: () => enrollmentsApi.listByStudent(studentId, { page: 0, size: 500 }),
    enabled: Boolean(studentId),
  })),
});
```

(Adjust API method names to match existing `enrollments-api.ts` / keys.)

- [ ] **Step 2: Stop filtering mosque-wide `useCircles` for PARENT**

Build circle rows from enrollment circle ids + `circlesApi.getById` (batched) **or** enrich enrollment response if it already includes circle name. Prefer: for each distinct ACTIVE `circleId`, fetch circle by id (already asserts). Do **not** call `useCircles(params)` which hits `pageForCaller` → empty for PARENT.

- [ ] **Step 3: Circles page** — ensure `ParentCirclesView` receives **student** ids from `useMyChildren()` (ids are student UUIDs), not ParentStudent link ids. Fix `circles-page.tsx` if it currently passes `profile?.parentStudentIds` incorrectly (confirm WorkspaceProfile field meaning in `WorkspaceService.fromParent`).

- [ ] **Step 4: Manual/typecheck — PARENT with 4 children sees all active circles; no Mark Attendance on this page.

- [ ] **Step 5: Skip commit**

---

### Task 5: Parent attendance student-scoped read + hide Mark

**Files:**
- Modify: `frontend/src/pages/app/attendance/attendance-page.tsx`
- Create (optional): `frontend/src/pages/app/attendance/parent-attendance-view.tsx`
- Verify: `useAttendanceByStudent` / attendance API `findByStudentId`

**Interfaces:**
- Consumes: `assertCanAccessStudent` path via `GET .../attendance/student/{id}` (confirm exact path in `AttendanceController`)
- Produces: PARENT sees linked children’s attendance; no Mark Attendance link

- [ ] **Step 1: Branch PARENT on AttendancePage** similar to STUDENT:

```tsx
const isParent = role === "PARENT";
if (isParent) {
  return <ParentAttendanceView />;
}
```

- [ ] **Step 2: ParentAttendanceView** — load children via `useMyChildren`, then `useAttendanceByStudent` per child (or selected child tabs). Display status/date/circle name. No link to `/attendance/circle/:id`.

- [ ] **Step 3: Staff path** — Mark Attendance button only when `role` is TEACHER | MOSQUE_ADMIN | SUPER_ADMIN (never PARENT).

- [ ] **Step 4: Typecheck / smoke** — PARENT does not see Mark CTA; linked child rows visible when data exists.

- [ ] **Step 5: Skip commit**

---

### Task 6: Role-affordance honesty on shared routes

**Files:**
- Modify: `frontend/src/lib/navigation/app-nav.ts`
- Modify: `frontend/src/lib/navigation/role-permissions.ts` (if present)
- Modify shared pages that still show create/edit for PARENT: students, enrollments, circles, attendance, admin chrome
- i18n only if new empty-state keys needed

**Interfaces:**
- Consumes: `normalizeApiRole`, existing `canManage*` helpers
- Produces: PARENT/STUDENT never see staff mutators on shared routes

- [ ] **Step 1: Audit nav items** — PARENT must not see mosque-admin-only entries; STUDENT similarly.
- [ ] **Step 2: Gate create buttons** with `canManageX(role)` already used on circles; apply same pattern wherever PARENT currently sees staff CTAs.
- [ ] **Step 3: Skip commit**

**P1 pause gate:** Foreign findById 403; PARENT children/circles/attendance readable; Mark hidden for PARENT; `getMyChildren` wired. Do not start P1b until this gate is green.

---

## Phase P1b — SUPER_ADMIN control plane

### Task 7: StuckWorkItem + fleet pending-joins hub

**Files:**
- Create: `backend/.../dtos/stuckwork/StuckWorkItem.java`, `StuckWorkResponse.java`
- Create: `backend/.../services/StuckWorkService.java`
- Create: `backend/.../controllers/v1/StuckWorkController.java` (`GET /api/v1/admin/stuck-work`, `@PreAuthorize("hasRole('SUPER_ADMIN')")`)
- Modify: `frontend/src/pages/app/admin/admin-dashboard-page.tsx` (+ small API module)

**StuckWorkItem fields:** `kind` (`PENDING_JOIN` | `BROKEN_PARENT_LINK` | …), `mosqueId`, `mosqueName`, `resourceId`, `summary`, `createdAt`, `densityScore`.

- [ ] **Step 1: Failing test** — SA GET stuck-work returns pending joins across mosques; MOSQUE_ADMIN → 403
- [ ] **Step 2: Implement service** — query pending `MosqueMemberJoinRequest` across tenants; map to items; sort by density
- [ ] **Step 3: FE hub list + drill-in link to mosque desk (`/mosques/:id` or existing admin surfaces)
- [ ] **Step 4: Skip commit**

### Task 8: Audit-reason gate on SA override writes

**Files:**
- Create migration `V9__add_override_audit_log.sql` (next free version — check `backend/src/main/resources/db/migration/`)
- Create entity `OverrideAuditLog` + repository + service method `record(actorId, mosqueId, action, reason, resourceType, resourceId)`
- Modify high-risk SA write entry points to require `X-Audit-Reason` header or body field `auditReason` (min length 8); reject if blank for SUPER_ADMIN cross-tenant / override paths

- [ ] **Step 1: Test** — SA override without reason → 400; with reason → 200 and row in audit table
- [ ] **Step 2: Implement gate + ledger
- [ ] **Step 3: Skip commit**

### Task 9: Capability chip (role + scope)

**Files:**
- Create: `frontend/src/components/shared/capability-chip.tsx`
- Modify app shell / layout header

- [ ] **Step 1: Chip shows** `PARENT · linked children` / `TEACHER · mosque` / `SUPER_ADMIN · platform + all mosques`
- [ ] **Step 2: Skip commit**

---

## Phase P2 — Teacher loop + excuse

### Task 10: Circle session mark + ACTIVE roster only

**Files:**
- Modify: `AttendanceService.create` — assert enrollment.circleId equals request.circleId; enrollment status ACTIVE; caller assigned teacher (already partially there)
- Modify: `frontend/.../circle-attendance-page.tsx` — roster from ACTIVE enrollments only
- Test: integration happy path + unauthorized teacher + withdrawn excluded

### Task 11: Excuse sets EXCUSED + FE client

**Files:**
- Modify: `AttendanceService.submitExcuse` — set `status` to `AttendanceStatus.EXCUSED` when excuse submitted
- Modify: `frontend/src/features/attendance/api/attendance-api.ts` — `submitExcuse(id, body)`
- Wire PARENT UI action on parent attendance rows; teacher circle view shows EXCUSED + reason
- Test: PARENT linked → 200 EXCUSED; foreign parent → 403

### Task 12: Student own attendance + enrollment approve names

**Files:**
- Confirm STUDENT path on attendance page (already present); ensure other-student denied on BE
- Enrollment admin UX: show student/circle names not raw UUIDs in approve/create dialogs

---

## Phase P3 — Admin + invite hygiene

### Task 13: Dual invite labels + mosque rotate

**Files:** mosque invite UI + BE rotate endpoint if missing; reject old code after rotate

### Task 14: Create-by-name search (teacher/student/circle)

**Files:** admin form dialogs — combobox by name, not clipboard UUID

### Task 15: ParentStudent admin create mosque assert

**Files:** `ParentStudentService.create(UUID callerId, ...)` — `assertCanAccessMosque(callerId, student.getMosque().getId())` for non-SA; SA requires audit reason (Task 8)

### Task 16: parentInviteCode rotate/expiry + student detail display

**Files:** Student entity/service + student detail page; forever-valid closed

### Task 17: Remove silent 3-child cap

**Files:** completed in Task 4 if `useQueries` over all children; verify PARENT with 4+ children

---

## Phase P4 — Polish + docs

### Task 18: Tables show names; nav honesty; dashboard pending count

### Task 19: Memorization/goals links from parent child context (link-scoped)

### Task 20: Annotate stale `docs/reviews/*` — open backlog = live items only

### Task 21: SA stuck-path diagnostic write rails (repair ParentStudent / invite / enrollment with audit; no impersonate)

---

## Parallel execution waves (this session)

| Wave | Tasks | File ownership (avoid conflicts) |
|------|-------|----------------------------------|
| W1 | Task 1 + Task 3 | BE IDOR trio vs FE API only |
| W2 | Task 2 ∥ Task 4 ∥ Task 5 | after Task 1 helpers exist; FE circles vs attendance vs BE audit |
| W3 | Task 6 | after parent pages stable |
| Pause | Verify P1 gate | |
| W4+ | P1b → P2 → P3 → P4 | per task file map |

---

## Test matrix (minimum)

Accounts: SUPER_ADMIN; MOSQUE_ADMIN A/B; TEACHER A1/A2; STUDENT S1 + S2 other mosque; PARENT P1 linked S1, P3 with 4+ children; PENDING joins.

Must-pass spine: invite → link → mark → parent sees → excuse → teacher sees.  
Must-pass SA: drill mosque A; override with audit; admin B never sees A; foreign findById denied for non-SA.

---

## Self-review (plan vs spec)

| Spec item | Task |
|-----------|------|
| P1-A IDOR | Task 1 |
| P1-B audit findById | Task 2 |
| P1-C parent circles | Task 4 |
| P1-D parent attendance | Task 5 |
| P1-E getMyChildren | Task 3 |
| P1-F affordances | Task 6 |
| P1b-A/B/C | Tasks 7–9 |
| P2-A–E | Tasks 10–12 |
| P3-A–F | Tasks 13–17 |
| P4-A–F | Tasks 18–21 |
| Out of scope | Honored in Global Constraints |
| No git | Global Constraints |

No TBD placeholders in P1 steps; later phases specify files and acceptance without inventing unavailable APIs.
