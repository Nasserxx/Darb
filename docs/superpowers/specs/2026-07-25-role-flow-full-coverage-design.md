# Darb Role-Flow Full Coverage Design

**Date:** 2026-07-25  
**Status:** Approved for implementation planning  
**Method:** ADHD divergent ideation (2 runs) + multi-agent role empathy + brainstorming

---

## 1. Goal

Make every Darb role’s core journeys complete and honest: what each person expects to **see**, **do**, and **how they react** when broken — plus clear **relations** between roles. Fix live logic, flow, and UX gaps. Do not re-fix stale list-leak findings already closed in code.

**Success:** Cross-role spine works end-to-end:

> Parent invite → Admin sees link → Teacher marks attendance → Parent sees status → Parent excuses → Teacher sees note  
> Plus: SUPER_ADMIN can operate any mosque and clear fleet stuck work with audited overrides.

---

## 2. Personas (lived expectations)

### 2.1 SUPER_ADMIN — whole-system controller

**Correction:** SUPER_ADMIN is **not** a hands-off fleet watcher. They control the **entire** system: every mosque, user, circle, attendance, parent link, invite, and break-glass action.

| | Expectation |
|---|-------------|
| **See** | Fleet hub (stuck work across mosques) + drill into any mosque as admin-equivalent + platform controls |
| **Do** | Cross-tenant create/edit/delete/recover; override marks, links, invites; appoint/replace mosque admins; rotate codes; every override logged (who/what/mosque/why) |
| **React** | IDOR/tenant bleed = platform fire; locked mosque = break-glass; parent stuck = repair link + correct invite type |
| **Relations** | Bootstrap and backstop MOSQUE_ADMIN; emergency fix for teacher/parent/student; then **hand the mosque back**. Do not habitually mark attendance as if assigned teacher. |

**I know it works when:** I can open any mosque without dead ends; overrides leave an audit trail; mosque admins stay isolated from each other; other roles keep working when I’m not in the room.

### 2.2 MOSQUE_ADMIN — one mosque operator

| | Expectation |
|---|-------------|
| **See** | Own mosque only; human names (not UUIDs); labeled dual invites |
| **Do** | Approve joins; create circles/teachers/students by name; issue/rotate mosque codes and student `parentInviteCode`; see parent–child links |
| **React** | UUID forms → stall/WhatsApp; forever codes → stop sharing; cross-mosque link risk → freeze that screen |
| **Relations** | Gives teachers mosque invite; gives parents **parent** invite (not mosque member code); escalates to SUPER_ADMIN for platform/tenant failures |

### 2.3 TEACHER — Maghrib loop

| | Expectation |
|---|-------------|
| **See** | My assigned circles; roster = kids in the room; excuses before mark |
| **Do** | Open → mark present/absent/late → save in under ~2 minutes |
| **React** | Ghost roster / missing excuses / save fail → paper attendance; abandon Darb |
| **Relations** | Expects admin to keep enrollment↔circle aligned; expects parents to excuse in-app before Maghrib |

### 2.4 PARENT — link-scoped guardian

| | Expectation |
|---|-------------|
| **See** | Children by **name**; their circles; attendance; progress |
| **Do** | Join via `parentInviteCode`; read; submit excuse — **never Mark Attendance**, never pick a mosque |
| **React** | Empty kids/circles, Mark→403, UUID names → WhatsApp mosque and abandon app |
| **Relations** | Trust teacher marks; contact admin for codes/links; not a mosque member |

### 2.5 STUDENT — learner

| | Expectation |
|---|-------------|
| **See** | My circles; my attendance; my progress; copyable `parentInviteCode` |
| **Do** | Join via mosque invite; share parent code with parent |
| **React** | Empty after join → “am I enrolled?”; failed parent code → blame app |
| **Relations** | Teacher marks truth; parent links via my invite; admin owns membership |

---

## 3. Relations & handoff artifacts

| Edge | Artifact | Rule |
|------|----------|------|
| SUPER_ADMIN → MOSQUE_ADMIN | Mosque + MosqueAdmin (+ admin invite) | Bootstrap / recover / override with audit |
| MOSQUE_ADMIN → TEACHER/STUDENT | Mosque join code / join request | Membership into one mosque |
| MOSQUE_ADMIN → PARENT | `parentInviteCode` on Student | Parent is **not** mosque member |
| PARENT → STUDENT | ParentStudent link | Link-scoped only (`mosque_id` never on PARENT user) |
| TEACHER → STUDENT | Enrollment → Attendance | Mark only if ACTIVE enrollment in assigned circle |
| PARENT → TEACHER | Excuse on Attendance | Write excuse only; read marks; never Mark |
| STUDENT → PARENT | Share `parentInviteCode` | Handoff to parent account |

**Non-edges:** PARENT → mosque-scoped write APIs; any non-SA → foreign tenant `findById` without assert.

---

## 4. Product invariants

1. **PARENT is link-scoped.** Access via ParentStudent → Student only. Never assign `mosque_id` to PARENT. Never route parent UX through `assertCanAccessMosque` / `pageForCaller`.
2. **Enrollment is the roster proof.** Teacher marks / parent sees a circle iff ACTIVE Enrollment(student, circle).
3. **Attendance verbs by role.** Mark = TEACHER (assigned circle). Excuse = PARENT (linked child). PARENT UI must never expose Mark.
4. **Dual invite types, both rotatable.** Mosque join code ≠ `parentInviteCode`. Labeled distinctly; expire/rotate; revoke invalidates use.
5. **Mosque assert on mosque-role mutations.** ParentStudent / Enrollment / Circle / Teacher creates fail closed across mosques.
6. **Every non-SA `findById` asserts.** Resource UUID is not a capability. SUPER_ADMIN may access all; writes require audit reason when overriding.
7. **Names at every handoff surface.** UUIDs are wire IDs only.
8. **No silent child cap** (or documented max with clear error).
9. **SUPER_ADMIN = whole-system controller.** Fleet stuck-work hub + mosque drill-in + audited cross-tenant CRUD — not platform-only hands-off.
10. **No half-shipped CTAs.** If API exists, FE is wired; else hide the button.

---

## 5. Live gaps (re-verified; stale docs discarded)

Treat `docs/reviews/*` list-leak P0s and “no parent invite” as **historical** where code already fixed them.

| ID | Severity | Gap |
|----|----------|-----|
| R1 | P0 | `findById` IDOR on attendance / enrollment / parent-student (and audit other by-id surfaces) |
| R2 | P0 | PARENT attendance: Mark CTA → circle API → `assertCanAccessMosque` → 403 |
| R3 | P0 | PARENT circles empty via `pageForCaller` |
| R4 | P0 | FE `parentStudentsApi.getMyChildren` missing while hook + BE exist |
| R5 | P1 | Excuse BE half-shipped (no FE; status not EXCUSED) |
| R6 | P1 | Admin ParentStudent create without mosque assert |
| R7 | P1 | Attendance create enrollment↔circle mismatch |
| R8 | P1 | Invite codes forever-valid / no rotate UX |
| R9 | P1 | UUID-heavy admin create forms; 3-child cap in ParentCirclesView |
| R10 | P1 | SUPER_ADMIN lacks stuck-work hub + audited override UX |

---

## 6. Architecture decisions

### 6.1 Parent data path (fog-of-war fix)

- Parent lists children via `getMyChildren`.
- Circles = distinct circles from children’s ACTIVE enrollments (student-scoped APIs).
- Attendance = student-scoped read (`findByStudentId` + `assertCanAccessStudent`).
- Hide Mark Attendance for PARENT; show Excuse when P2 ships.

### 6.2 SUPER_ADMIN control plane

- **Home:** Stuck-work hub — union of pending joins, broken parent links, invite SLA, empty-circle symptoms, security incidents; ranked by stuck density.
- **Drill-in:** Open any mosque desk (admin-equivalent surfaces) under SA context.
- **Overrides:** Cross-tenant mutate allowed; **mandatory audit reason** on override/cross-tenant writes.
- **Stuck-path diagnostics:** Open a named parent/student friction surface as read-clone + repair rails (ParentStudent, invite rotate, enrollment) — **no impersonation session**.
- **Capability chip:** Persistent role chip; SA = “platform + all mosques”; mosque operand required on mosque-scoped verbs.

### 6.3 Role-affordance honesty

Shared routes that list PARENT must declare staff vs parent ownership. Illegal CTAs are **absent**, not greyed. Align nav, page CTAs, and API asserts.

---

## 7. Implementation phases

Hard pause after P1 for authz verification before expanding surfaces.

### Phase P1 — Security + parent unblock

| ID | Item | Acceptance |
|----|------|------------|
| P1-A | Guard findById IDOR (Attendance, Enrollment, ParentStudent) | Foreign UUID → 403/404; tests per role |
| P1-B | Audit other public findById surfaces | Assert or unexpose |
| P1-C | Parent circles via enrollments-by-student | No `pageForCaller` for PARENT; names; no Mark CTA |
| P1-D | Parent attendance student-scoped read | Hide Mark; linked child visible; foreign 403 |
| P1-E | Wire `getMyChildren` in FE API client | Hook succeeds; names shown |
| P1-F | Role-affordance honesty on shared routes | PARENT cannot see staff mutators |

### Phase P1b — SUPER_ADMIN control plane (after P1 security green)

| ID | Item | Acceptance |
|----|------|------------|
| P1b-A | StuckWorkItem + fleet pending-joins hub | SA home shows cross-mosque stuck joins + drill-in |
| P1b-B | Audit-reason gate on SA override writes | Override without reason rejected; ledger stores actor/mosque/why |
| P1b-C | Capability chip (role + scope) | Illegal CTAs hidden; SA chip shows platform scope |

### Phase P2 — Teacher loop + excuse

| ID | Item | Acceptance |
|----|------|------------|
| P2-A | Circle session mark for assigned teacher | Happy path + unauthorized denied |
| P2-B | Roster = ACTIVE enrollments only | Withdrawn/pending excluded |
| P2-C | Excuse sets EXCUSED + FE client | PARENT/authorized submit; teacher sees status |
| P2-D | Student own attendance read | Self ok / other denied |
| P2-E | Enrollment approve UX with names | No UUID hunting |

### Phase P3 — Admin + invite hygiene

| ID | Item | Acceptance |
|----|------|------------|
| P3-A | Dual invite UX copy/labels | Mosque vs parent code never confused |
| P3-B | Persist/retrieve/rotate mosque invite codes | Old code rejected after rotate |
| P3-C | Create teacher/student/circle by name search | No clipboard UUIDs |
| P3-D | ParentStudent admin create mosque assert | Cross-mosque link fails |
| P3-E | parentInviteCode expiry/rotate + show on student detail | Forever-valid closed |
| P3-F | Remove silent 3-child cap | All linked children appear |

### Phase P4 — Polish + docs

| ID | Item | Acceptance |
|----|------|------------|
| P4-A | Tables show names | UUID secondary only |
| P4-B | Nav role honesty | STUDENT/PARENT no fake mosque admin chrome |
| P4-C | Dashboard pending join count | Admin badge/stat |
| P4-D | Memorization/goals links from student/parent child context | Link-scoped for PARENT |
| P4-E | Annotate stale review docs | Open backlog = live items only |
| P4-F | SA stuck-path diagnostic write rails | Repair parent/student dead-ends with audit; no impersonate |

---

## 8. Explicitly out of scope

- WebSockets / realtime push  
- Impersonation / “act as user” session  
- Bulk attendance mark  
- Excuse `actingAs` dual-hat schema (single `UserRole` today; revisit later)  
- Assigning `mosque_id` to PARENT  
- Re-fixing stale list-mosque leaks  
- Payments polish, activity feeds, register-time admin invite merge  

---

## 9. Test matrix (minimum accounts)

1. SUPER_ADMIN  
2. MOSQUE_ADMIN A + B (two mosques)  
3. TEACHER A1 (circle C1), TEACHER A2 (circle C2)  
4. STUDENT S1 (ACTIVE enrollment), S2 other mosque  
5. PARENT P1 (linked S1), P2 second guardian, P3 (4+ children)  
6. PENDING / REJECTED join requests  

**Must-pass spine:** invite → link → mark → parent sees → excuse → teacher sees.  
**Must-pass SA:** drill into mosque A; override with audit; mosque B admin never sees A’s lists; foreign findById denied for non-SA.

---

## 10. ADHD shortlist (design drivers)

1. ★ Parent fog-of-war → student-scoped truth (fixes R2/R3)  
2. ★ SA stuck-work hub + drill-in + audit (whole-system controller)  
3. Role-affordance / capability chips (hide illegal CTAs)  
4. Stuck-path SA diagnostics without impersonation  
5. PARENT `nextLegalAction` contract (no empty dead-end screens)  

**Rejected traps:** Forced empathy rituals before SA work; markets metaphors for seating/excuses; daytime-only SA write windows; giving PARENT a mosque.

---

## 11. Next step

After user review of this spec: invoke **writing-plans** to produce a phased implementation plan (P1 → pause → P1b → P2 → P3 → P4), then implement with tests per phase.
