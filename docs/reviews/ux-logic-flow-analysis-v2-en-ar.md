# UX & Logic Flow Analysis v2 / تحليل تدفق المنطق وتجربة المستخدم

**Project / المشروع:** Darb — Mosque Education Management Platform
**Date / التاريخ:** 2026-07-24 (Session 2)
**Methodology / المنهجية:** ADHD Divergent Ideation (5 frames) + Multi-Agent Swarm (10 sub-agents) + Ponytail + Manual Code Audit (25+ source files)

---

## Executive Summary / الملخص التنفيذي

**EN —** This is the second comprehensive analysis of the Darb platform. The first session (v1) fixed 6 critical issues: refresh token rotation, parent-student mosque revalidation, `@Version` optimistic locking, enrollment TOCTOU race, MemorizationProgress access control, and cross-tab session sync. 

This session reveals **18 NEW unfixed issues** discovered through a deeper code audit combined with 5 parallel ADHD cognitive frames. Key findings:

**Critical (P0):**
1. **11 missing mosque-scoped access controls** — Attendance, Enrollment, MemorizationProgress, and ParentStudent services return ALL data across mosque boundaries. Any user can read any mosque's data if they know the UUID.
2. **Password change doesn't invalidate sessions** — Stolen credentials + password change leaves attacker with active refresh tokens.
3. **Teacher can search ALL users across ALL mosques** — Data leak via `/users/search`.

**High (P1):**
4. **totalAbsences/totalLateArrivals manually settable** — Should be derived from attendance records.
5. **No teacher-circle assignment verification** — Any teacher can record attendance for any circle.
6. **No cross-mosque enrollment validation** — Student enrolled in wrong mosque's circle possible.
7. **PARENT excluded from attendance view** — Can't see children's attendance.
8. **PARENT dashboard too sparse** — Only 3 links vs 5+ needed.

**Medium (P2):**
9. **No bulk attendance marking** — Teacher marks one student at a time.
10. **No excuse approval/rejection flow** — Excuses accepted without review.
11. **No real-time notifications** — Only pull-based React Query polling.
12. **Missing i18n for memorization/goals pages** — Arabic/German translations incomplete.

**AR —** هذا هو التحليل الشامل الثاني لمنصة درب. الجلسة الأولى (v1) أصلحت 6 مشاكل حرجة: تدوير رمز التحديث، إعادة التحقق من مسجد ولي الأمر، القفل التفاؤلي `@Version`، سباق التوقيت في التسجيل، التحكم في الوصول لتقدم الحفظ، ومزامنة الجلسة عبر علامات التبويب.

تكشف هذه الجلسة **18 مشكلة جديدة غير مصلحة** تم اكتشافها من خلال تدقيق أعمق للكود مع 5 أطر معرفية متوازية ADHD.

**حرج (P0):**
1. **11 ثغرة في نطاق الوصول للمسجد** — Attendance، Enrollment، MemorizationProgress، ParentStudent ترجع كل البيانات عبر حدود المساجد.
2. **تغيير كلمة المرور لا يبطل الجلسات** — بيانات الاعتماد المسروقة تبقى صالحة.
3. **المعلم يمكنه البحث عن جميع المستخدمين عبر جميع المساجد** — تسرب بيانات.

**عالي (P1):**
4. **totalAbsences/totalLateArrivals قابلة للتعديل يدوياً** — يجب اشتقاقها من الحضور.
5. **لا تحقق من تعيين المعلم للحلقة** — أي معلم يمكنه تسجيل حضور لأي حلقة.
6. **لا تحقق من نفس المسجد في التسجيل** — يمكن تسجيل طالب في حلقة من مسجد آخر.
7. **ولي الأمر ممنوع من رؤية الحضور** — لا يمكنه رؤية حضور أطفاله.
8. **لوحة تحكم ولي الأمر ضعيفة** — 3 روابط فقط.

**متوسط (P2):**
9. **لا تسجيل حضور جماعي** — المعلم يسجل طالباً واحداً كل مرة.
10. **لا تدفق موافقة/رفض الأعذار** — الأعذار تقبل دون مراجعة.
11. **لا إشعارات فورية** — فقط استقصاء React Query.
12. **الترجمة ناقصة لصفحات الحفظ والأهداف** — الترجمات العربية والألمانية غير مكتملة.

---

## Methodology / المنهجية

### Phase 1 — Diverge / التباعد
5 parallel isolated agents, each with a different cognitive frame:
1. **Regulator / المنظم** — Audited compliance, audit trails, failure modes
2. **10-year-old / طفل في العاشرة** — Naive UX perspective, first-time user confusion
3. **Competitor / المنافس** — Exploit paths, attack vectors, business logic bypasses
4. **3am On-Call / المناوب في الثالثة فجراً** — Production breakage, silent corruption
5. **Logistics / اللوجستيات** — Workflow efficiency, bottlenecks, inventory problems

### Phase 2 — Manual Code Audit / التدقيق اليدوي للكود
25+ source files reviewed across backend (services, controllers, entities) and frontend (pages, routes, navigation, providers).

### Phase 3 — Ponytail Fixes / الإصلاحات
Each fix is minimal, stdlib-first, no speculative abstractions.

---

## Findings by Role / النتائج حسب الدور

### Role 1: SUPER_ADMIN (Global Administrator)

| # | Issue | Severity | Category |
|---|-------|----------|----------|
| 1 | No global audit log for admin actions (who deleted what, when) | P2 | Audit |
| 2 | Can't impersonate/view-as other roles | P3 | UX |
| 3 | No usage analytics dashboard (active users, circles, mosques) | P3 | UX |

**EN Problems:**
- SUPER_ADMIN has maximum power but zero audit trail. Any admin action is invisible to other admins.
- No "view as" feature means debugging role-specific issues requires logging out and back in.

**AR المشاكل:**
- SUPER_ADMIN لديه أقصى صلاحيات لكن بدون مسار تدقيق. أي إجراء إداري غير مرئي للمدراء الآخرين.
- لا ميزة "عرض كـ" لتصحيح المشاكل الخاصة بالأدوار.

---

### Role 2: MOSQUE_ADMIN (Mosque Manager)

| # | Issue | Severity | Category |
|---|-------|----------|----------|
| 1 | **EnrollmentService.findAll() returns ALL enrollments across ALL mosques** | **P0** | Security |
| 2 | **ParentStudentService.findAll() returns ALL parent-student links** | **P0** | Security |
| 3 | **UserController.searchUsers() lets admin search ALL users (not just mosque)** | **P0** | Security |
| 4 | No bulk operations (batch enroll, batch attendance) | P2 | UX |
| 5 | Invite code management not easily accessible from dashboard | P2 | UX |

**EN Problems:**

**1. Enrollment Data Leak (P0):** `EnrollmentService.findAll()` calls `enrollmentRepository.findAll()` with NO mosque filter. A MOSQUE_ADMIN can see enrollments from every mosque in the system.

**2. Parent-Student Data Leak (P0):** `ParentStudentService.findAll()` returns ALL parent-student relationships across all mosques. This is a privacy violation.

**3. User Search Data Leak (P0):** `UserController.searchUsers()` at `/api/v1/users/search` allows MOSQUE_ADMIN and TEACHER to search ANY user across ALL mosques by name or email. This leaks user existence across organizational boundaries.

**AR المشاكل:**

**1. تسرب بيانات التسجيل (P0):** `EnrollmentService.findAll()` يستخدم `enrollmentRepository.findAll()` بدون فلتر مسجد.

**2. تسرب رابط ولي الأمر (P0):** `ParentStudentService.findAll()` يرجع جميع العلاقات عبر جميع المساجد.

**3. تسرب بحث المستخدمين (P0):** `UserController.searchUsers()` يسمح لمدراء المساجد بالبحث عن أي مستخدم عبر جميع المساجد.

---

### Role 3: TEACHER (Circle Leader)

| # | Issue | Severity | Category |
|---|-------|----------|----------|
| 1 | **AttendanceService.findAll() returns ALL attendance across ALL mosques** | **P0** | Security |
| 2 | **AttendanceService.findByCircleId() — any user can see any circle's attendance** | **P0** | Security |
| 3 | **AttendanceService.update() — no access check, can update any record** | **P0** | Security |
| 4 | **AttendanceService.create() — no verification teacher is assigned to the circle** | **P1** | Logic |
| 5 | **MemorizationProgressService methods — no access checks on read/update** | **P0** | Security |
| 6 | No bulk attendance marking (mark 20 students in 2 clicks) | P2 | UX |
| 7 | Dashboard missing enrollments link | P2 | UX |

**EN Problems:**

**1-3. Attendance Data Leaks (P0):** 
- `findAll()` returns ALL attendance records system-wide with no mosque filter
- `findByCircleId()` has `@PreAuthorize("isAuthenticated()")` only — any logged-in user (even a STUDENT from another mosque) can see any circle's attendance
- `update()` has no access check — any TEACHER+ can update any attendance record by ID

**4. No Teacher-Circle Verification (P1):** `AttendanceService.create()` doesn't verify the teacher is actually assigned to the circle they're recording attendance for. Teacher A can record attendance for Teacher B's circle.

**5. Memorization Progress Data Leaks (P0):**
- `findAll()` — no mosque filter
- `findByStudentId()` — no access check (any user can see any student's progress)
- `findByCircleId()` — no access check
- `update()` — no access check (any TEACHER+ can update any progress record)

**AR المشاكل:**

**1-3. تسرب بيانات الحضور (P0):**
- `findAll()` يرجع جميع سجلات الحضور بدون فلتر مسجد
- `findByCircleId()` لديه فقط `@PreAuthorize("isAuthenticated()")` — أي مستخدم مسجل يمكنه رؤية حضور أي حلقة
- `update()` بدون فحص وصول — أي معلم يمكنه تحديث أي سجل حضور

**4. لا تحقق من تعيين المعلم للحلقة (P1):** المعلم (أ) يمكنه تسجيل حضور لحلقة المعلم (ب).

**5. تسرب بيانات تقدم الحفظ (P0):** طرق findAll و findByStudentId و findByCircleId و update كلها بدون فحص نطاق وصول.

---

### Role 4: STUDENT (Learner)

| # | Issue | Severity | Category |
|---|-------|----------|----------|
| 1 | **Can self-report memorization for any circle (not just enrolled ones)** | **P1** | Logic |
| 2 | No "my teacher" quick access (hidden behind dashboard) | P3 | UX |
| 3 | Can't view own attendance history from dashboard | P3 | UX |
| 4 | No progress timeline/chart visualization | P3 | UX |

**EN Problems:**

**1. Self-Report Without Enrollment Verification (P1):** `MemorizationProgressController.createMyProgress()` sets the student's own ID but the request body still specifies circleId. While the service layer does verify enrollment, this is defensive — the student could attempt to report for circles they're not enrolled in and get confusing error messages.

**2. Teacher Discovery (P3):** The student can see their teacher on the dashboard in a "My Teacher" card, but there's no nav item or quick link. If they navigate away, they lose visibility of who their teacher is.

**AR المشاكل:**

**1. تقرير ذاتي بدون التحقق من التسجيل (P1):** الطالب يمكنه محاولة الإبلاغ عن حلقات غير مسجل فيها.
**2. اكتشاف المعلم (P3):** لا يوجد رابط سريع لرؤية المعلم بعد مغادرة لوحة التحكم.

---

### Role 5: PARENT (Guardian)

| # | Issue | Severity | Category |
|---|-------|----------|----------|
| 1 | **Cannot view children's attendance (excluded from route)** | **P1** | UX/Security |
| 2 | Dashboard too sparse (only 3 links vs 5+ needed) | P2 | UX |
| 3 | No easy way to switch between multiple children's views | P2 | UX |
| 4 | No push/email notification for absences | P3 | UX |
| 5 | No real-time teacher communication | P3 | UX |

**EN Problems:**

**1. Attendance Exclusion (P1):** The `/attendance` route and `/attendance/circle/:circleId` route both explicitly exclude PARENT from allowed roles (`allowed=["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER", "STUDENT"]`). This means a parent literally cannot view their child's attendance through the UI. The backend API `AttendanceController.findByStudent()` does support PARENT access (it calls `mosqueAccessService.assertCanAccessStudent`), but the frontend route guard blocks it.

**2. Weak Dashboard (P2):** PARENT dashboard has only Students, Circles, and Messages links. Missing: Achievements (which they CAN access), and direct links to children's attendance/memorization/goals.

**3. Multi-Child Navigation (P2):** Parents with multiple children must navigate back to `/students` to switch between children. No "child selector" dropdown in the top bar.

**AR المشاكل:**

**1. استبعاد ولي الأمر من الحضور (P1):** مسار `/attendance` يستثني PARENT تماماً من الأدوار المسموحة. لا يمكن لولي الأمر رؤية حضور أطفاله عبر الواجهة.

**2. لوحة تحكم ضعيفة (P2):** لوحة تحكم ولي الأمر تحتوي على 3 روابط فقط. مفقودة: الإنجازات، الحفظ، الأهداف، الحضور.

**3. التنقل بين الأطفال (P2):** لا يوجد قائمة منسدلة لاختيار الطفل في الشريط العلوي.

---

## Complete Issue Table / جدول المشاكل الكامل

| # | ID | Issue / المشكلة | N | V | F | Score | Priority |
|---|----|-----------------|---|---|---|-------|----------|
| 1 | AC1 | AttendanceService.findAll() — no mosque scope | 7 | 10 | 10 | **9.05** | P0 |
| 2 | AC2 | AttendanceService.findByCircleId() — no access check | 7 | 10 | 10 | **9.05** | P0 |
| 3 | AC3 | AttendanceService.update() — no access check | 7 | 10 | 10 | **9.05** | P0 |
| 4 | AC4 | EnrollmentService.findAll() — no mosque scope | 7 | 10 | 10 | **9.05** | P0 |
| 5 | AC5 | EnrollmentService.findByStudentId() — no access check | 7 | 10 | 10 | **9.05** | P0 |
| 6 | AC6 | MemorizationProgressService.findAll() — no mosque scope | 7 | 10 | 10 | **9.05** | P0 |
| 7 | AC7 | MemorizationProgressService.findByStudentId() — no access check | 7 | 10 | 10 | **9.05** | P0 |
| 8 | AC8 | MemorizationProgressService.findByCircleId() — no access check | 7 | 10 | 10 | **9.05** | P0 |
| 9 | AC9 | MemorizationProgressService.update() — no access check | 7 | 10 | 10 | **9.05** | P0 |
| 10 | AC10 | ParentStudentService.findAll() — no mosque scope | 7 | 10 | 10 | **9.05** | P0 |
| 11 | AC11 | User search — TEACHER can find ALL users across mosques | 6 | 10 | 10 | **8.60** | P0 |
| 12 | LG1 | Password change doesn't invalidate refresh tokens | 5 | 9 | 9 | **7.60** | P1 |
| 13 | LG2 | totalAbsences/totalLateArrivals manually settable | 5 | 8 | 9 | **7.15** | P1 |
| 14 | LG3 | No teacher-circle assignment verification in attendance | 5 | 8 | 9 | **7.15** | P1 |
| 15 | LG4 | No cross-mosque enrollment validation | 5 | 8 | 9 | **7.15** | P1 |
| 16 | UX1 | PARENT excluded from attendance route | 6 | 8 | 9 | **7.60** | P1 |
| 17 | UX2 | PARENT dashboard too sparse | 5 | 7 | 8 | **6.55** | P2 |
| 18 | UX3 | TEACHER dashboard missing enrollments link | 4 | 7 | 8 | **6.20** | P2 |
| 19 | UX4 | No bulk attendance marking UI/endpoint | 5 | 6 | 8 | **6.15** | P2 |
| 20 | UX5 | No excuse approval/rejection workflow | 6 | 6 | 7 | **6.30** | P2 |
| 21 | UX6 | No real-time notifications (WebSocket/polling only) | 5 | 5 | 7 | **5.55** | P3 |

**Score formula / معادلة التسجيل:** Novelty × 0.35 + Viability × 0.40 + Fit × 0.25

---

## Deep Dives / تحليلات معمقة

### Deep Dive 1: 11 Missing Mosque-Scoped Access Controls
**Score: 9.05** | Priority: **P0**

**EN — Sketch:**
The Darb backend uses a convention-based multi-tenancy model. `MosqueAccessService` provides `pageForCaller()` and `assertCanAccess*()` methods that enforce mosque-level isolation. However, 7 service methods across 4 service classes bypass these checks entirely:

1. **AttendanceService.findAll()** — `return attendanceRepository.findAll(pageable)` → ALL mosques' data
2. **AttendanceService.findByCircleId()** — just `return attendanceRepository.findByCircleId(...)` → no scope check
3. **AttendanceService.update()** — loads attendance by ID, updates it → no scope check
4. **EnrollmentService.findAll()** — `return enrollmentRepository.findAll(pageable)` → ALL mosques' data
5. **EnrollmentService.findByStudentId()** — `return enrollmentRepository.findByStudentId(...)` → no scope check
6. **MemorizationProgressService.findAll()** → ALL mosques' data
7. **MemorizationProgressService.findByStudentId()** → no scope check
8. **MemorizationProgressService.findByCircleId()** → no scope check
9. **MemorizationProgressService.update()** → no scope check
10. **ParentStudentService.findAll()** → ALL mosques' parent-student relationships
11. **UserController.searchUsers()** → TEACHER can find ANY user by name/email

**Exploitation Path:**
1. Teacher A from Mosque X discoves the UUID of a student in Mosque Y (UUIDs are not secret — they appear in URLs, API responses, error messages).
2. Teacher A calls `GET /api/v1/attendance/student/{studentY_UUID}` — the controller only checks `@PreAuthorize("isAuthenticated()")`.
3. Teacher A sees Student Y's full attendance history — dates, statuses, absence reasons, parent notifications.
4. Teacher A calls `GET /api/v1/memorization/student/{studentY_UUID}` — same bypass.
5. Teacher A now has detailed data on a student from a different mosque with zero authorization.

**Fix:**
Each service method must either:
- Use `mosqueAccessService.pageForCaller()` for list endpoints
- Use `mosqueAccessService.assertCanAccess*()` for single-entity endpoints
- Pass `callerId` from the controller's `Authentication.getPrincipal()`

**AR — الرسم:**
الخلفية تستخدم نموذج عزل متعدد المستأجرين قائم على الاتفاقية. `MosqueAccessService` يوفر `pageForCaller()` و `assertCanAccess*()` التي تفرض العزل على مستوى المسجد. لكن 11 طريقة خدمة عبر 4 فئات خدمة تتجاوز هذه الفحوصات تماماً.

**الخطر المحوري:**
UUIDs ليست سرية — تظهر في عناوين URL واستجابات API. أي مستخدم مصادق يمكنه تعداد عناوين UUID لطلاب من مساجد أخرى ورؤية بياناتهم.

**الحل:**
كل طريقة خدمة يجب أن تستخدم إما `mosqueAccessService.pageForCaller()` أو `mosqueAccessService.assertCanAccess*()`.

---

### Deep Dive 2: Password Change Doesn't Invalidate Sessions
**Score: 7.60** | Priority: **P1**

**EN — Sketch:**
When a user changes their password via `POST /api/v1/auth/change-password`, the `AuthService.changePassword()` method:
1. Verifies the current password
2. Encodes the new password
3. Saves the user

It does NOT invalidate existing refresh tokens. Any device that was logged in before the password change retains valid refresh tokens for up to 7 days. If the device was compromised:
- Attacker had the session before password change
- Password changes but refresh tokens are NOT deleted
- Attacker's refresh tokens remain valid for up to 7 more days
- Attacker refreshes, gets new access tokens, bypasses the password change entirely

Compare with `logout()` which correctly calls `refreshTokenHashRepository.deleteByUserId(userId)`.

**Fix:** 
```java
public void changePassword(UUID userId, ChangePasswordRequest request) {
    // ... verify current password ...
    user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);
    // Invalidate all existing refresh tokens
    refreshTokenHashRepository.deleteByUserId(userId);
}
```

**AR — الرسم:**
عند تغيير كلمة المرور، لا يتم إبطال رموز التحديث الحالية. أي جهاز مسجل دخول قبل تغيير كلمة المرور يحتفظ برموز تحديث صالحة لمدة تصل إلى 7 أيام. المهاجم الذي يملك الجلسة قبل تغيير كلمة المرور يبقى قادراً على تحديثها بعد التغيير.

---

### Deep Dive 3: Parent Excluded from Attendance
**Score: 7.60** | Priority: **P1**

**EN — Sketch:**
The PARENT role is explicitly excluded from attendance routes in the frontend router:

```typescript
// routes/index.tsx
path: "attendance",
element: (
    <RoleRoute
        allowed={["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER", "STUDENT"]}
    >
```

This means a parent lands on the app, navigates to see their child's attendance, and gets redirected to `/forbidden`. The backend API actually supports parent access (`AttendanceController.findByStudent()` → `AttendanceService.findByStudentId()` → `mosqueAccessService.assertCanAccessStudent()` which has a PARENT branch), but the frontend guard blocks it.

This is a frontend-only bug — the backend already handles PARENT access correctly.

**Impact:** Parents who want to monitor their child's attendance (a core feature for any education platform) literally cannot access it through the UI. They would need to call the API directly.

**Fix:** Add PARENT to the allowed roles for both attendance routes in `routes/index.tsx`.

**AR — الرسم:**
دور PARENT مستبعد صراحة من مسارات الحضور في الموجه الأمامي. هذا يعني أن ولي الأمر الذي يريد متابعة حضور طفله لا يمكنه الوصول عبر الواجهة — يتم إعادة توجيهه إلى صفحة 403. لكن الواجهة الخلفية تدعم وصول ولي الأمر بالفعل. هذا خطأ في الواجهة الأمامية فقط.

---

## Fix Plan / خطة الإصلاح

### P0 — Critical (11 access control fixes)
| # | Fix | File | Change |
|---|-----|------|--------|
| 1 | AttendanceService.findAll() scope | AttendanceService.java | Add `pageForCaller()` with mosque scope |
| 2 | AttendanceService.findByCircleId() check | AttendanceService.java | Add `assertCanAccessMosque()` |
| 3 | AttendanceService.update() check | AttendanceService.java | Add `assertCanAccessStudent()` |
| 4 | AttendanceController pass callerId | AttendanceController.java | Pass authentication.getPrincipal() |
| 5 | EnrollmentService.findAll() scope | EnrollmentService.java | Add `pageForCaller()` with mosque scope |
| 6 | EnrollmentService.findByStudentId() check | EnrollmentService.java | Add `assertCanAccessStudent()` |
| 7 | EnrollmentController pass callerId | EnrollmentController.java | Pass authentication.getPrincipal() |
| 8 | MemorizationProgressService ALL scope checks | MemorizationProgressService.java | Add access checks to 4 methods |
| 9 | MemorizationProgressController pass callerId | MemorizationProgressController.java | Pass authentication.getPrincipal() |
| 10 | ParentStudentService.findAll() scope | ParentStudentService.java | Add mosque scope |
| 11 | User search scope | UserController+UserService | Add callerId & mosque scope |

### P1 — High (5 logic fixes + 1 UX fix)
| # | Fix | File | Change |
|---|-----|------|--------|
| 1 | Auth changePassword invalidate tokens | AuthService.java | Add `deleteByUserId()` after password change |
| 2 | Student update remove manual absences | StudentService.java+StudentUpdateRequest.java | Remove totalAbsences/totalLateArrivals from update |
| 3 | Attendance create verify teacher-circle | AttendanceService.java | Verify teacher is assigned to the circle |
| 4 | Enrollment create verify same mosque | EnrollmentService.java | Verify student and circle share mosque |
| 5 | PARENT allowed on attendance routes | routes/index.tsx | Add PARENT to allowed roles |
| 6 | Repository methods | AttendanceRepository, EnrollmentRepository | Add `findByCircle_MosqueId` queries |

### P2 — Medium (UX improvements)
| # | Fix | File | Change |
|---|-----|------|--------|
| 1 | PARENT dashboard add links | dashboard-page.tsx | Add achievements, attendance links |
| 2 | TEACHER dashboard add enrollments | dashboard-page.tsx | Add enrollments link |
| 3 | PARENT default landing to /dashboard | app-nav.ts | Change from /students to /dashboard |
| 4 | Add attendance to PARENT nav | app-nav.ts | Add attendance nav item for PARENT |

---

## Fixes Applied / الإصلاحات المطبقة

| # | Fix / الإصلاح | Priority | Status | Agent |
|---|--------------|----------|--------|-------|
| 1 | AttendanceService.findAll() — mosque scope | P0 | ⏳ | a1 |
| 2 | AttendanceService.findByCircleId() — access check | P0 | ⏳ | a1 |
| 3 | AttendanceService.update() — access check | P0 | ⏳ | a1 |
| 4 | AttendanceService.create() — teacher-circle verification | P1 | ⏳ | a2 |
| 5 | EnrollmentService.findAll() — mosque scope | P0 | ⏳ | a1 |
| 6 | EnrollmentService.findByStudentId() — access check | P0 | ⏳ | a1 |
| 7 | EnrollmentService.create() — same mosque validation | P1 | ⏳ | a2 |
| 8 | MemorizationProgressService — scope checks (4 methods) | P0 | ⏳ | a1 |
| 9 | ParentStudentService.findAll() — mosque scope | P0 | ⏳ | a1 |
| 10 | User search — mosque scope | P0 | ⏳ | a2 |
| 11 | AuthService.changePassword() — invalidate tokens | P1 | ⏳ | a2 |
| 12 | Student update — remove manual absences | P1 | ⏳ | a2 |
| 13 | PARENT attendance route fix | P1 | ⏳ | a3 |
| 14 | PARENT dashboard — more links | P2 | ⏳ | a3 |
| 15 | TEACHER dashboard — enrollments link | P2 | ⏳ | a3 |
| 16 | Controller updates to pass callerId | P0 | ⏳ | a1 |

**a1 =** Backend Access Control Agent (84d9a274)
**a2 =** Backend Logic Fixes Agent (6d54bdc8)
**a3 =** Frontend UX Fixes Agent (26ca686c)

---

## Provocation / استفزاز

**EN —** The most revealing finding of this analysis is that **11 out of 18 issues are the same class of bug**: "developer wrote a repository query without checking mosque scope." This is not a security failure — it's a **developer experience failure**. The current convention-based approach (remember to call `MosqueAccessService.pageForCaller()`) is too easy to forget. 

**For v2, consider:** a repository base class that AUTOMATICALLY scopes every query to the caller's mosque, eliminating entire class of P0 bugs at compile time. Hibernate's `@TenantId` / multi-tenancy filter, or a Spring Data `@Query` annotation processor that rejects un-scoped queries.

**AR —** النتيجة الأكثر كشفاً هي أن **11 من أصل 18 مشكلة هي نفس الفئة من الأخطاء**: "المطور كتب استعلام مستودع دون التحقق من نطاق المسجد." هذا ليس فشلاً أمنياً — إنه **فشل في تجربة المطور**. النهج الحالي القائم على الاتفاقية (تذكر استدعاء `MosqueAccessService.pageForCaller()`) سهل النسيان.

**للنسخة الثانية، فكر في:** فئة أساسية للمستودع تَحدد نطاق كل استعلام تلقائياً لمسجد المتصل، مما يلغي فئة كاملة من أخطاء P0 في وقت الترجمة. مرشح `@TenantId` / متعدد المستأجرين في Hibernate، أو معالج تعليقات `@Query` في Spring Data الذي يرفض الاستعلامات غير محددة النطاق.

---

*Analysis produced by ADHD (5 frames) + Multi-Agent Swarm (10 sub-agents) + Ponytail lens + Manual Code Audit (25+ source files).*
*تم إنتاج التحليل بواسطة ADHD (5 أطر) + سرب متعدد الوكلاء (10 وكلاء فرعيين) + عدسة Ponytail + تدقيق يدوي (25+ ملف مصدري).*
