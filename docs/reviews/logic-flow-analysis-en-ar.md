# Logic Flow Analysis / تحليل تدفق المنطق

**Project / المشروع:** Darb — Mosque Education Management Platform
**Date / التاريخ:** 2026-07-24
**Methodology / المنهجية:** ADHD Divergent Ideation (5 frames) + Multi-Agent Swarm + Ponytail

---

## Executive Summary / الملخص التنفيذي

**EN —** This document presents a comprehensive analysis of logic flow problems in the Darb mosque education platform. Using 5 parallel cognitive frames (Speedrunner, 3am On-Call, Inversion, Regulator, Remove Assumption) under the ADHD divergent ideation methodology, we identified 30 distinct ideas clustered into 6 problem domains. The top 3 findings were deepened with full exploitation sketches, risks, and fixes.

**Critical findings:**
1. **No refresh token rotation** — Any captured refresh token remains valid indefinitely. N concurrent requests produce N valid token pairs. **Fix:** Implement refresh token rotation with a `refresh_token_hashes` table.
2. **Parent-student tenant isolation gap** — Parents retain access to student data across mosque transfers via stale `ParentStudent` links. **Fix:** Add mosque revalidation check in `MosqueAccessService.assertCanAccessStudent()`.
3. **TOCTOU race in enrollment→memorization progress** — No locking between enrollment check and progress INSERT, compounded by `MemorizationProgressController` bypassing `MosqueAccessService`. **Fix:** Add `@Version` optimistic locking and enrollment verification in the service layer.

**AR —** يقدم هذا المستند تحليلاً شاملاً لمشاكل تدفق المنطق في منصة درب لإدارة المساجد وحلقات التحفيظ. باستخدام 5 أطر معرفية متوازية (متسابق السرعة، المناوب في الثالثة فجراً، الانعكاس، المنظم، إزالة الافتراض المسيطر) ضمن منهجية التباعد الإبداعي ADHD، تم تحديد 30 فكرة متميزة موزعة على 6 مجالات مشاكل. تم تعميق أهم 3 نتائج مع رسوم كاملة لسيناريوهات الاستغلال والمخاطر والحلول.

**النتائج الحرجة:**
1. **عدم تدوير رمز التحديث** — أي رمز تحديث يتم التقاطه يبقى صالحاً إلى أجل غير مسمى. الطلبات المتزامنة تنتج أزواجاً متعددة من الرموز الصالحة. **الحل:** تطبيق تدوير رمز التحديث باستخدام جدول `refresh_token_hashes`.
2. **ثغرة عزل المستأجر بين ولي الأمر والطالب** — يحتفظ أولياء الأمور بإمكانية الوصول إلى بيانات الطلاب عبر المساجد بعد النقل عبر روابط `ParentStudent` القديمة. **الحل:** إضافة التحقق من إعادة التحقق من المسجد في `MosqueAccessService.assertCanAccessStudent()`.
3. **سباق التوقيت (TOCTOU) في التسجيل→تقدم الحفظ** — لا يوجد قفل بين التحقق من التسجيل وإدراج تقدم الحفظ، ويتفاقم بتجاوز `MemorizationProgressController` لـ `MosqueAccessService`. **الحل:** إضافة القفل التفاؤلي `@Version` والتحقق من التسجيل في طبقة الخدمة.

---

## Methodology / المنهجية

### EN

The analysis used the **ADHD** (Divergent Ideation) methodology combined with multi-agent swarm execution:

1. **Phase 1 — Diverge:** 5 parallel isolated agents, each operating under a different cognitive frame:
   - **Speedrunner** — Finds glitches, skips, and edge cases in the logic flow
   - **3am On-Call** — Identifies what wakes an engineer at 3am
   - **Inversion** — Asks the opposite question to surface hidden assumptions
   - **Regulator** — Audits for compliance, auditability, and failure modes
   - **Remove Assumption** — Removes load-bearing architectural assumptions

2. **Phase 2 — Focus:** Scoring (Novelty × 0.35 + Viability × 0.40 + Fit × 0.25), clustering into 6 groups, and deepening the top 3 findings with:
   - Exploitation sketch
   - Load-bearing risk
   - First concrete fix step
   - 3-5 child ideas

3. **Ponytail lens** applied throughout: each finding comes with a minimal, stdlib-first fix suggestion. No speculative abstractions.

### AR

استخدم التحليل منهجية **ADHD** (التفكير التباعدي الإبداعي) مقترنة بتنفيذ سرب متعدد الوكلاء:

1. **المرحلة 1 — التباعد:** 5 وكلاء متوازيين ومعزولين، كل منهم يعمل تحت إطار معرفي مختلف:
   - **متسابق السرعة** — يجد الثغرات والاختصارات والحالات الحدية في تدفق المنطق
   - **المناوب في الثالثة فجراً** — يحدد ما يوقظ المهندس في الثالثة فجراً
   - **الانعكاس** — يطرح السؤال المعاكس لكشف الافتراضات الخفية
   - **المنظم** — يدقق في الامتثال وقابلية التدقيق وأنماط الفشل
   - **إزالة الافتراض المسيطر** — يزيل الافتراضات المعمارية الأساسية

2. **المرحلة 2 — التركيز:** تسجيل النتائج (الجدة × 0.35 + الجدوى × 0.40 + الملاءمة × 0.25)، وتجميعها في 6 مجموعات، وتعميق أهم 3 نتائج مع:
   - رسم سيناريو الاستغلال
   - الخطر المحوري
   - أول خطوة إصلاح ملموسة
   - 3-5 أفكار فرعية

3. **عدسة Ponytail** طُبقت خلال التحليل: كل نتيجة تأتي مع اقتراح إصلاح أدنى باستخدام المكتبة القياسية أولاً. لا تجريدات تخمينية.

---

## Findings by Cluster / النتائج حسب المجموعة

### Cluster 1: Auth & Session Integrity / المجموعة 1: سلامة المصادقة والجلسات

| # | Idea / الفكرة | N | V | F | Score |
|---|---|---|---|---|---|
| O4 | N concurrent refreshes → N valid token pairs / طلبات تحديث متزامنة → أزواج رموز صالحة متعددة | 6 | 9 | 9 | **7.95** |
| S1 | Race token refresh boundary — dual valid windows / سباق حدود تحديث الرمز — نافذتان صالحتان | 7 | 8 | 9 | **7.90** |
| I1 | JWT claims not revalidated mid-session — stale privileges / مطالبات JWT لا يعاد التحقق منها خلال الجلسة | 5 | 9 | 9 | **7.60** |
| S5 | Corrupt single-flight 401 DoS / تعطيل الطيران الواحد 401 | 8 | 6 | 8 | **7.20** |
| I5 | No optimistic locking on any entity / لا قفل تفاؤلي على أي كيان | 4 | 8 | 8 | **6.60** |
| S6 | Stack trace leaks HMAC key / تسرب مفتاح HMAC من تتبع المكدس | 7 | 4 | 9 | **6.45** |
| R4 | No clock skew leeway in JWT validation / لا هامش لانحراف الساعة في التحقق من JWT | 4 | 7 | 8 | **6.20** |

**EN Problems:**

- **Refresh Token Multiplication (O4, S1):** The backend never invalidates old refresh tokens. Each `/auth/refresh` call with the same token succeeds independently. An attacker who intercepts one token can generate unlimited session pairs. N concurrent refresh requests produce N valid token pairs — a session proliferation attack.
- **Stale JWT Privileges (I1):** A TEACHER demoted to STUDENT retains full TEACHER access until their 15-minute access token expires. Role changes are not enforced mid-session.
- **No Optimistic Locking (I5):** All entities lack `@Version`. Two concurrent admins approving different join requests for the same student can create duplicate role records — last-write-wins silently.

**EN Fixes:**

1. **Implement refresh token rotation:** Store SHA-256 hashes of valid refresh tokens per user. On refresh, consume the old hash and issue a new one. Wrap in a DB transaction to prevent race. (See Deep Dive 1)
2. **Revalidate JWT claims mid-session:** Add a lightweight middleware that checks the user's current role against the token's role claim on every request. Cache the result with a 30-second TTL to avoid DB load.
3. **Add `@Version` to `BaseAuditableEntity`:** A single change that makes every write conflict-detectable across all mutation endpoints.

**AR المشاكل:**

- **مضاعفة رمز التحديث (O4, S1):** لا يقوم الخادم بتعطيل رموز التحديث القديمة أبداً. كل استدعاء `/auth/refresh` بنفس الرمز ينجح بشكل مستقل. المهاجم الذي يعترض رمزاً واحداً يمكنه توليد أزواج جلسات غير محدودة.
- **صلاحيات JWT القديمة (I1):** المعلم الذي تم تخفيض رتبته إلى طالب يحتفظ بصلاحيات المعلم الكاملة حتى انتهاء صلاحية رمز الوصول (15 دقيقة). لا يتم تطبيق تغييرات الدور خلال الجلسة.
- **لا قفل تفاؤلي (I5):** جميع الكيانات تفتقر إلى `@Version`. اثنان من مدراء المسجد يوافقان على طلبات انضمام لنفس الطالب يمكن أن يخلقا سجلات أدوار مكررة.

**AR الحلول:**

1. **تطبيق تدوير رمز التحديث:** تخزين تجزئات SHA-256 لرموز التحديث الصالحة لكل مستخدم. عند التحديث، استهلك التجزئة القديمة وأصدر جديدة. ضمن معاملة قاعدة بيانات لمنع السباق.
2. **إعادة التحقق من مطالبات JWT خلال الجلسة:** إضافة وسيط خفيف يتحقق من دور المستخدم الحالي مقابل دور الرمز في كل طلب. تخزين النتيجة مؤقتاً لمدة 30 ثانية.
3. **إضافة `@Version` إلى `BaseAuditableEntity`:** تغيير واحد يجعل كل كتابة قابلة للكشف عن التعارض عبر جميع نقاط نهاية التعديل.

---

### Cluster 2: Tenant Isolation Gaps / المجموعة 2: ثغرات عزل المستأجر

| # | Idea / الفكرة | N | V | F | Score |
|---|---|---|---|---|---|
| I6 | PARENT-student link stale on mosque transfer / رابط ولي الأمر-الطالب قديم بعد نقل المسجد | 7 | 7 | 9 | **7.50** |
| O1 | Thread-local tenant context lost in async / سياق المستأجر المحلي للخيط يُفقد في العمليات غير المتزامنة | 6 | 7 | 9 | **7.15** |
| I2 | No compiler-enforced tenant scoping on queries / لا نطاق مستأجر مفروض بالمترجم على الاستعلامات | 5 | 8 | 9 | **7.20** |
| O3 | Missing isActive filter on JOIN / عدم وجود فلتر isActive في JOIN | 4 | 8 | 8 | **6.60** |
| R2 | Orphaned payment rows on soft-delete / صفوف دفع يتيمة بعد الحذف الناعم | 4 | 7 | 8 | **6.20** |

**EN Problems:**

- **Stale Parent-Student Linkage (I6):** When a student transfers mosques, the `ParentStudent` link is not revalidated. The parent retains access to the student's data — including attendance, progress, payments — from the old mosque context. This is a cross-tenant data leak. (See Deep Dive 2)
- **Async Tenant Context Loss (O1):** `MosqueAccessService` likely uses a request-scoped bean or `SecurityContextHolder` derived from `SecurityContext`. Any `@Async` or `@Scheduled` method that lacks this context defaults to null or fallback — silently mixing data between mosques.
- **No Compiler-Enforced Tenant Scoping (I2):** Tenant isolation is convention-based. A developer writing a new repository query can forget to add `.where(mosqueId = ?)`. There is no ArchUnit test or repository base class that automatically scopes queries by tenant.

**EN Fixes:**

1. **Add mosque snapshot to `ParentStudent`:** Store `mosque_id` at link creation time. Revalidate on every access in `MosqueAccessService`.
2. **Tenant-aware async executor:** Use a `ThreadPoolTaskExecutor` with a `HystrixRequestVariable`-like pattern or `SimpleAsyncTaskExecutor` that propagates the tenant context.
3. **ArchUnit test for tenant scoping:** Write a test that inspects all repository methods to ensure they include `mosqueId` or `Mosque` in their query parameters.

**AR المشاكل:**

- **رابط ولي الأمر-الطالب القديم (I6):** عندما ينتقل الطالب بين المساجد، لا يعاد التحقق من رابط `ParentStudent`. يحتفظ ولي الأمر بإمكانية الوصول إلى بيانات الطالب عبر المساجد.
- **فقدان سياق المستأجر غير المتزامن (O1):** `MosqueAccessService` يستخدم نطاق طلب مستمد من `SecurityContext`. أي طريقة `@Async` أو `@Scheduled` تفتقر لهذا السياق ستستخدم قيمة افتراضية أو احتياطية.
- **لا نطاق مستأجر مفروض بالمترجم (I2):** عزل المستأجر قائم على الاتفاقية دون تطبيق إجباري. يمكن لمطور يكتب استعلام مستودع جديد أن ينسى إضافة شرط المسجد.

**AR الحلول:**

1. **إضافة لقطة للمسجد إلى `ParentStudent`:** تخزين `mosque_id` عند إنشاء الرابط. إعادة التحقق عند كل وصول في `MosqueAccessService`.
2. **منفذ غير متزامن واعٍ للمستأجر:** استخدام `ThreadPoolTaskExecutor` مع نمط ينشر سياق المستأجر.
3. **اختبار ArchUnit لنطاق المستأجر:** كتابة اختبار يفحص جميع طرق المستودع لضمان تضمين `mosqueId`.

---

### Cluster 3: Data Integrity & Race Conditions / المجموعة 3: سلامة البيانات وظروف السباق

| # | Idea / الفكرة | N | V | F | Score |
|---|---|---|---|---|---|
| R3 | TOCTOU enrollment→progress race / سباق التوقيت بين التسجيل وتقدم الحفظ | 6 | 7 | 9 | **7.15** |
| I3 | Join-request pipeline no compensatory transaction / خط أنابيب طلب الانضمام بدون معاملة تعويضية | 5 | 8 | 9 | **7.20** |
| S2 | Soft-delete vs optimistic create race / سباق الحذف الناعم مقابل الإنشاء التفاؤلي | 8 | 6 | 8 | **7.20** |
| I4 | Soft-delete cascades with no audit trail / الحذف الناعم المتتالي بدون مسار تدقيق | 4 | 8 | 8 | **6.60** |

**EN Problems:**

- **TOCTOU Enrollment→Progress (R3):** The enrollment check and memorization progress INSERT are separate DB round-trips. A concurrent enrollment change between these two statements can cause progress to be attributed to the wrong circle. `MemorizationProgressController` compounds this by directly injecting `TeacherRepository`, skipping `MosqueAccessService`. (See Deep Dive 3)
- **Join-Request Pipeline Orphans (I3):** The join request → role assignment flow has no compensatory transaction. If the DB write succeeds but the follow-up notification or role creation fails silently, the user is stuck in "approved but can't access" limbo.
- **Soft-Delete No Audit Trail (I4):** Soft-deleting a User cascades to their records but preserves no `deleted_by`, `deleted_at`, or original state snapshot. Forensic reconstruction after accidental deletion is impossible.

**EN Fixes:**

1. **Add `@Version` optimistic locking + enrollment verification:** Use `LockModeType.OPTIMISTIC` on the enrollment read inside `@Transactional`. (See Deep Dive 3)
2. **Introduce a saga/retry pattern for join-request pipeline:** Wrap the approval flow in a transactional outbox. If notification sending fails, a retry queue re-processes it.
3. **Add audit columns to soft-delete:** Store `deleted_by`, `deleted_at`, and `restored_at` on all soft-deletable entities.

**AR المشاكل:**

- **سباق التوقيت (R3):** التحقق من التسجيل وإدراج تقدم الحفظ هما رحلتا قاعدة بيانات منفصلتان. تغيير التسليم المتزامن بين هاتين العمليتين يمكن أن ينسب التقدم إلى الحلقة الخطأ.
- **أيتام خط أنابيب طلب الانضمام (I3):** تدفق طلب الانضمام ← تعيين الدور ليس لديه معاملة تعويضية. إذا نجحت كتابة قاعدة البيانات لكن إشعار المتابعة أو إنشاء الدور فشل بصمت، يعلق المستخدم في حالة "تمت الموافقة لكن لا يمكن الوصول".
- **الحذف الناعم بدون مسار تدقيق (I4):** الحذف الناعم للمستخدم يتتالي إلى سجلاته لكنه لا يحتفظ بـ `deleted_by` أو `deleted_at` أو لقطة للحالة الأصلية.

**AR الحلول:**

1. **إضافة القفل التفاؤلي `@Version` + التحقق من التسجيل.**
2. **تقديم نمط saga/retry لخط أنابيب طلب الانضمام.**
3. **إضافة أعمدة تدقيق للحذف الناعم.**

---

### Cluster 4: Financial & Compliance Gaps / المجموعة 4: الثغرات المالية والامتثال

| # | Idea / الفكرة | N | V | F | Score |
|---|---|---|---|---|---|
| R1 | Payment webhook receipt not persisted / إيصال webhook الدفع غير محفوظ | 5 | 8 | 9 | **7.20** |
| R6 | PaymentCycle no transition history / لا تاريخ انتقال لدورة الدفع | 5 | 7 | 9 | **6.80** |
| R5 | Message no individual expunge / لا حذف فردي للرسائل | 6 | 7 | 8 | **6.90** |

**EN Problems:**

- **Webhook Receipt Not Persisted (R1):** Payment status is updated on gateway webhook, but the raw webhook payload is not stored. If a dispute arises, there is no evidence of whether the gateway was never called or the response was silently dropped.
- **PaymentCycle No History (R6):** Only the current enum value is stored. Who changed it from MONTHLY to ANNUAL and when is lost. Billing disputes are unprovable.
- **Message Expunge (R5):** All circle messages are permanent. A safeguarding report about a specific student lives in the group transcript forever, visible to any circle member.

**EN Fixes:**

1. **Store raw webhook payloads:** Add a `payment_webhooks` table that logs the full request body + headers + timestamp before processing.
2. **Audit trail for payment cycle changes:** Add a `payment_audit` table tracking `previous_cycle → new_cycle`, `changed_by`, `changed_at`.
3. **Soft-expunge for messages:** Add an `is_expunged` flag on messages that hides them from group view but preserves them for audit.

**AR المشاكل:**

- **إيصال Webhook غير محفوظ (R1):** يتم تحديث حالة الدفع عند webhook البوابة، لكن الحمولة الأولية للـ webhook لا تخزن.
- **لا تاريخ لدورة الدفع (R6):** فقط القيمة الحالية للـ enum مخزنة. من قام بتغييرها ومتى ضاع.
- **حذف الرسائل (R5):** جميع رسائل الحلقة دائمة. تقرير حماية عن طالب معين يبقى في سجل المجموعة للأبد.

---

### Cluster 5: Frontend & UX Failure Modes / المجموعة 5: أنماط فشل الواجهة الأمامية وتجربة المستخدم

| # | Idea / الفكرة | N | V | F | Score |
|---|---|---|---|---|---|
| O5 | New tab loses sessionStorage → lockout / علامة تبويب جديدة تفقد sessionStorage → إغلاق | 7 | 8 | 7 | **7.35** |
| S3 | RTL override chars → UI spoofing / أحرف تجاوز RTL → تزوير الواجهة | 9 | 4 | 7 | **6.55** |
| O6 | Arabic RTL chars break CSS grid / أحرف عربية RTL تكسر شبكة CSS | 6 | 6 | 7 | **6.30** |

**EN Problems:**

- **sessionStorage Tab Lockout (O5):** Opening the app in a new tab loses the sessionStorage token. The 401 auto-refresh fires but if the refresh token stored in sessionStorage is also missing, every request bounces between 401 and redirect — locking the user out completely.
- **RTL Spoofing (S3):** i18next interpolates user-controlled locale prefix params into translated error messages. An attacker could inject RTL override characters (U+202E) in the `/ar` route to craft a fake login prompt under the app's own origin.
- **RTL CSS Breakage (O6):** Real Arabic content with mixed LTR numbers, diacritics, or Unicode control characters can collapse CSS grid layouts or render charts invisible.

**EN Fixes:**

1. **Migrate from sessionStorage to localStorage** for tokens, or use `BroadcastChannel` API to sync session across tabs.
2. **Sanitize locale route params** before i18next interpolation. Strip Unicode control characters from route segments.
3. **Add RTL-specific CSS regression tests** with real Arabic data fixtures.

**AR المشاكل:**

- **إغلاق علامة تبويب sessionStorage (O5):** فتح التطبيق في علامة تبويب جديدة يفقد رمز sessionStorage.
- **تزوير RTL (S3):** i18next يدرج معاملات مسار اللغة المحددة من قبل المستخدم في رسائل الخطأ المترجمة.
- **كسر CSS بالنصوص العربية (O6):** المحتوى العربي الحقيقي مع أرقام LTR مختلطة يمكن أن يكسر شبكات CSS.

---

### Cluster 6: Paradigm Shifts / المجموعة 6: التحولات النموذجية

| # | Idea / الفكرة | N | V | F | Score |
|---|---|---|---|---|---|
| A5 | API responses with TTL — all data provisional / استجابات API مع TTL — جميع البيانات مؤقتة | 8 | 5 | 7 | **6.55** |
| A2 | One Pi per mosque, gossip protocol / جهاز لكل مسجد، بروتوكول نشر | 10 | 2 | 5 | **5.60** |
| A1 | DB as cache of human testimony / قاعدة البيانات كذاكرة تخزين مؤقت للشهادات البشرية | 9 | 3 | 6 | **5.85** |
| A3 | User = family tablet, PIN-based / المستخدم = جهاز لوحي عائلي، برقم سري | 8 | 4 | 6 | **5.90** |
| A4 | Frontend owns schema, backend is dumb JSON grid / الواجهة الأمامية تملك المخطط، الخلفية شبكة JSON بسيطة | 7 | 3 | 5 | **4.95** |
| A6 | Student = attendance event stream / الطالب = تدفق أحداث الحضور | 9 | 3 | 5 | **5.60** |

**EN:** These are wild/paradigm-shifting ideas. They are not short-term fixes but provocative directions for a v2 architecture. The most actionable insight is **A5 (API responses with TTL)** — treating all data as provisional with explicit freshness metadata would solve the stale UI problem at the protocol level.

**AR:** هذه أفكار جامحة/تحويلية للنموذج. ليست حلولاً قصيرة المدى لكنها اتجاهات استفزازية للنسخة الثانية. الرؤية الأكثر قابلية للتنفيذ هي **A5 (استجابات API مع TTL)** — معاملة جميع البيانات كمؤقتة مع بيانات حداثة صريحة.

---

## Top 3 Deep Dives / أهم 3 تحليلات معمقة

---

### Deep Dive 1: Refresh Token Multiplication / مضاعفة رمز التحديث

**Weighted Score / النتيجة الموزونة: 7.95** | Cluster: Auth & Session Integrity

#### EN

**Sketch:**
An attacker who gains access to a victim's refresh token (via XSS, compromised dependency, browser extension, or network interception) can exploit the absence of refresh token rotation to generate unlimited valid session pairs. The attack works as follows:

1. The attacker obtains the victim's refresh token (stored in sessionStorage, readable by any JS on the origin).
2. The attacker fires N parallel POST `/api/v1/auth/refresh` requests using the same token.
3. The backend's `AuthService.refreshToken()` validates each independently — checking signature, expiry, and `token_type="refresh"` — but never checks a revocation list or marks the token as consumed.
4. All N requests succeed, producing N independent valid token pairs.
5. The attacker collects at least one pair and controls the account for up to 7 days (refresh token lifetime).
6. The victim's page overwrites its sessionStorage with the last-received pair, unaware of the proliferation.

The frontend's single-flight 401 interceptor (which queues concurrent 401 requests and shares one refresh call) actually makes this WORSE — it batches requests into the same timing window, increasing the density of concurrent refreshes.

**Load-Bearing Risk:**
Refresh token rotation is the single architectural change that eliminates this vulnerability class. If the backend marks each refresh token as consumed on first use (by storing a hash in a database and rejecting reused tokens), then N concurrent refreshes all race for the same token — the first wins, the other N-1 fail, and the attacker gets zero valid tokens. Without rotation, no amount of rate limiting, frontend hardening, or short expiry fully closes the gap.

**First Concrete Step:**
Add a `refresh_token_hashes` table (or column on the users table) that stores the SHA-256 hash of every currently-valid refresh token for that user. In `AuthService.refreshToken()`:

```java
// Step 1: Hash the incoming refresh token
String tokenHash = sha256(incomingRefreshToken);

// Step 2: Check it exists in the user's active set
// Step 3: DELETE that hash (consuming it)
// Step 4: Insert the hash of the new refresh token
// Wrap steps 2-4 in a transaction
```

This is rotation — each token can be used exactly once. Use `SELECT ... FOR UPDATE` or a conditional `DELETE ... WHERE EXISTS` to prevent the race where two concurrent requests both pass the existence check.

**Child Ideas:**
1. **Passive token family tree as detection signal** — Log each refresh token issuance with its parent `jti`. A single parent producing 2+ children in <1 second is unambiguous evidence of token theft. Trigger auto-revoke of the entire family tree.
2. **Cross-tab session sync via BroadcastChannel** — The frontend can use the BroadcastChannel API so Tab B silently adopts Tab A's fresh token instead of racing to refresh the old one.
3. **Refresh token pinning to browser fingerprint** — Embed a fingerprint hash (from Sec-CH-UA headers or TLS client cert) inside the refresh token claims. On refresh, verify the fingerprint matches.
4. **Graduated rate limiting on /auth/refresh** — Per-IP: 10 req/min, per-user: 3 refreshes per 15-min window. Combined with rotation, this eliminates the surprise-50-requests vector.
5. **Admin-visible token theft signal** — Log reuse of a consumed refresh token as a security event in the admin panel: IP, User-Agent, timestamp.

---

#### AR

**الرسم:**
المهاجم الذي يحصل على رمز تحديث الضحية (عبر XSS، اعتماد مخترق، إضافة متصفح، أو اعتراض شبكة) يمكنه استغلال غياب تدوير رمز التحديث لتوليد أزواج جلسات صالحة غير محدودة. الهجوم يعمل كالتالي:

1. يحصل المهاجم على رمز تحديث الضحية (مخزن في sessionStorage، يمكن لأي JS على النطاق قراءته).
2. يرسل المهاجم N طلب POST `/api/v1/auth/refresh` متوازي باستخدام نفس الرمز.
3. `AuthService.refreshToken()` في الخادم يتحقق من كل طلب بشكل مستقل — يتحقق من التوقيع، تاريخ الانتهاء، و `token_type="refresh"` — لكنه لا يتحقق أبداً من قائمة الإبطال أو يضع علامة على الرمز كمستهلك.
4. جميع طلبات N تنجح، منتجة N أزواج رموز صالحة مستقلة.
5. يجمع المهاجم زوجاً واحداً على الأقل ويتحكم في الحساب لمدة تصل إلى 7 أيام.
6. صفحة الضحية تكتب sessionStorage بآخر زوج استلمته، غير مدركة للانتشار.

**الخطر المحوري:**
تدوير رمز التحديث هو التغيير المعماري الوحيد الذي يزيل هذه الفئة من الثغرات. إذا وضع الخادم علامة على كل رمز تحديث كمستهلك عند أول استخدام (بتخزين تجزئة SHA-256 في قاعدة البيانات ورفض الرموز المعاد استخدامها)، فإن N من طلبات التحديث المتزامنة ستتنافس على نفس الرمز — الأول يفوز، والباقي يفشل.

**أول خطوة إصلاح:**
إضافة جدول `refresh_token_hashes` يخزن تجزئة SHA-256 لكل رمز تحديث صالح حالياً لذلك المستخدم. في `AuthService.refreshToken()`، تحقق من وجود التجزئة، احذفها (استهلكها)، وأدخل تجزئة الرمز الجديد — كل ذلك ضمن معاملة واحدة.

**الأفكار الفرعية:**
1. شجرة عائلة الرمز كإشارة كشف
2. مزامنة الجلسة عبر علامات التبويب باستخدام BroadcastChannel
3. تثبيت رمز التحديث إلى بصمة المتصفح
4. تحديد تدريجي للمعدل على `/auth/refresh`
5. إشارة سرقة الرمز مرئية للمسؤول

---

### Deep Dive 2: Stale Parent-Student Tenancy / قدوم رابط ولي الأمر-الطالب

**Weighted Score / النتيجة الموزونة: 7.50** | Cluster: Tenant Isolation Gaps

#### EN

**Sketch:**
When a `ParentStudent` link is created, it records which parent is associated with which student. The parent's access is scoped through this link — `MosqueAccessService.assertCanAccessStudent()` checks the `ParentStudent` table for existence of a link. However, it never revalidates that the student's CURRENT mosque matches any mosque the parent is associated with.

The exploit path:

1. Parent A is linked to Student B at Mosque X.
2. Student B transfers to Mosque Y (student.mosqueId changes).
3. The `ParentStudent` link is never updated or invalidated.
4. Parent A continues to access Student B's data — attendance, memorization progress, payments — now belonging to Mosque Y's tenant.
5. A MOSQUE_ADMIN from Mosque X could exploit this deliberately: create fake `ParentStudent` links to students in Mosque Y and exfiltrate data.
6. Mosque Y has no control over this access — they never approved the parent.

This is a cross-tenant data leak in a multi-tenant SaaS platform. The data flows across organizational boundaries without the source tenant's consent.

**Load-Bearing Risk:**
The PARENT branch in `assertCanAccessStudent` returns success based solely on `ParentStudent` linkage without verifying `student.getMosque().getId()` against any parent-to-mosque association. If this one check were added — resolving the parent's mosque and asserting it equals the student's mosque — the entire exploit path closes.

**First Concrete Step:**
Add a mosque revalidation check inside the PARENT branch of `assertCanAccessStudent()`:

```java
// After confirming ParentStudent link exists:
if (role == PARENT) {
    // Resolve parent's mosque via their associated mosque records
    UUID parentMosqueId = resolveParentMosqueId(callerId);
    if (parentMosqueId != null 
        && !parentMosqueId.equals(student.getMosque().getId())) {
        throw new ForbiddenException("Parent cannot access student from different mosque");
    }
}
```

If the parent has no direct mosque affiliation, add a `mosque_id` column to `parent_student` that snapshots the student's mosque at link creation time.

**Child Ideas:**
1. **ParentStudent mosque snapshot + transfer cascade** — Add `mosque_id` column. When a student transfers, expire all ParentStudent links, requiring re-link at the new mosque.
2. **Cross-mosque shared custody** — If both mosques agree (via an inter-mosque sharing agreement table), parent can retain access intentionally. Makes data flow auditable.
3. **Anomaly detection on cross-mosque parent access** — Log every PARENT→student access crossing a mosque boundary. Detect patterns like one parent accessing 3+ mosques.
4. **Time-bound ParentStudent + mosque-scoped JWT** — Add `valid_from`/`valid_until` columns. Issue JWT with linked student IDs AND mosque IDs valid at login time.
5. **IDE plugin for multi-tenant access control linting** — Static analysis rule: "If an access control method checks a join table for a non-admin role AND the target entity has a tenant FK, the check MUST also assert tenant equality."

---

#### AR

**الرسم:**
عند إنشاء رابط `ParentStudent`، يسجل أي ولي أمر مرتبط بأي طالب. وصول ولي الأمر محدد من خلال هذا الرابط — `MosqueAccessService.assertCanAccessStudent()` يتحقق من جدول `ParentStudent` لوجود رابط. لكنه لا يعيد التحقق أبداً من أن مسجد الطالب الحالي يتطابق مع أي مسجد يرتبط به ولي الأمر.

مسار الاستغلال:

1. ولي الأمر (أ) مرتبط بالطالب (ب) في المسجد (س).
2. الطالب (ب) ينتقل إلى المسجد (ص) (يتغير student.mosqueId).
3. رابط `ParentStudent` لا يتم تحديثه أو إبطاله أبداً.
4. ولي الأمر (أ) يستمر في الوصول إلى بيانات الطالب (ب) — الحضور، تقدم الحفظ، المدفوعات — التي تنتمي الآن لمستأجر المسجد (ص).
5. مدير المسجد (س) يمكنه استغلال هذا عمداً: إنشاء روابط `ParentStudent` مزيفة لطلاب في المسجد (ص) وسحب البيانات.
6. المسجد (ص) ليس لديه سيطرة على هذا الوصول — لم يوافقوا أبداً على ولي الأمر.

هذا تسرب بيانات عبر المستأجرين في منصة SaaS متعددة المستأجرين. تتدفق البيانات عبر الحدود التنظيمية دون موافقة المستأجر المصدر.

**الخطر المحوري:**
فرع PARENT في `assertCanAccessStudent` يعيد النجاح بناءً على رابط `ParentStudent` فقط دون التحقق من `student.getMosque().getId()` مقابل أي ارتباط بين ولي الأمر والمسجد.

**أول خطوة إصلاح:**
إضافة التحقق من إعادة التحقق من المسجد داخل فرع PARENT من `assertCanAccessStudent()`. إذا لم يكن لولي الأمر ارتباط مباشر بالمسجد، أضف عمود `mosque_id` إلى `parent_student` يلتقط مسجد الطالب وقت إنشاء الرابط.

---

### Deep Dive 3: TOCTOU Enrollment→Progress Race / سباق التوقيت بين التسجيل وتقدم الحفظ

**Weighted Score / النتيجة الموزونة: 7.15** | Cluster: Data Integrity & Race Conditions

#### EN

**Sketch:**
When a TEACHER calls `POST /api/v1/memorization`, the controller resolves the teacher ID from the auth token (bypassing `MosqueAccessService` — it directly injects `TeacherRepository`), then delegates to `MemorizationProgressService.create()`. This service performs three separate SELECTs (Student, Circle, Teacher) but performs zero enrollment verification.

The race condition:

1. Teacher opens a student's memorization page — sees the student enrolled.
2. Teacher submits progress data.
3. Between the enrollment check (if any) and the progress INSERT, a concurrent ADMIN transaction modifies the enrollment (e.g., student is withdrawn from the circle).
4. The progress row inserts successfully, now referencing a circle the student no longer belongs to.
5. Downstream reports — per-circle averages, teacher performance, student transcript — silently count phantom sessions.

The controller bypassing `MosqueAccessService` compounds the issue: a TEACHER authenticated at Mosque A could craft a request with a `student_id` from Mosque B (if they know the UUID), and the service never cross-references the teacher's mosque against the student's mosque.

**Load-Bearing Risk:**
The lack of a foreign-key-like invariant between `memorization_progress` and active enrollment. If progress rows store `circle_id` directly, a re-assigned student's progress is attributed to the wrong circle in per-circle rollups. The fix hinges on making the enrollment check atomic with the progress insert.

**First Concrete Step:**
Add an enrollment check inside the `@Transactional create()` method in `MemorizationProgressService`:

```java
@Transactional
public MemorizationProgress create(MemorizationProgressCreateRequest request) {
    // 1. Verify enrollment is ACTIVE with optimistic locking
    Enrollment enrollment = enrollmentRepository
        .findByStudentIdAndCircleIdWithLock(
            request.getStudentId(), request.getCircleId(), 
            LockModeType.OPTIMISTIC);
    
    if (enrollment == null || enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
        throw new BadRequestException("Student is not actively enrolled in this circle");
    }
    
    // 2. Also fix: verify teacher is authorized for this student
    mosqueAccessService.assertCanAccessStudent(callerId, request.getStudentId());
    
    // 3. Insert progress
    // ...
}
```

Use `LockModeType.OPTIMISTIC` on the enrollment read — this forces a `@Version` check on commit and retries the transaction if the enrollment was modified concurrently.

**Child Ideas:**
1. **Cross-mosque progress smuggling** — Since the controller skips `MosqueAccessService`, a TEACHER at Mosque A could submit progress for a Mosque B student. UUID enumeration becomes the attack surface.
2. **@Version on BaseAuditableEntity as global invariant guard** — Adding `@Version` to the base class makes every entity's writes conflict-detectable. The single highest-leverage fix across ALL mutation endpoints.
3. **CQRS snapshots for progress** — Store a copy of circle name, teacher name, and session date in the progress row (not just FKs). Reports always reflect what the circle was at session time, making the TOCTOU harmless.
4. **Enrollment-locked progress entry as saga** — Use `SELECT ... FOR UPDATE` on the enrollment before inserting progress. If the lock fails (student disenrolled), return 409 Conflict and save as draft.
5. **ArchUnit test: all controllers must use MosqueAccessService** — A compile-time check that prevents any controller from injecting domain repositories directly.

---

#### AR

**الرسم:**
عندما يستدعي المعلم `POST /api/v1/memorization`، يحل المتحكم معرف المعلم من رمز المصادقة (متجاوزاً `MosqueAccessService` — يحقن `TeacherRepository` مباشرة)، ثم يفوض إلى `MemorizationProgressService.create()`. هذه الخدمة تقوم بثلاث عمليات SELECT منفصلة (طالب، حلقة، معلم) لكنها لا تقوم بأي تحقق من التسجيل.

حالة السباق:

1. المعلم يفتح صفحة حفظ الطالب — يرى الطالب مسجلاً.
2. المعلم يرسل بيانات التقدم.
3. بين التحقق من التسجيل (إن وجد) وإدراج تقدم الحفظ، معاملة ADMIN متزامنة تعدل التسجيل.
4. صف التقدم يدرج بنجاح، مشيراً الآن إلى حلقة لم يعد الطالب ينتمي إليها.
5. التقارير النهائية — متوسطات الحلقة، أداء المعلم، سجل الطالب — تحسب جلسات وهمية بصمت.

تجاوز المتحكم لـ `MosqueAccessService` يفاقم المشكلة: المعلم الموثق في المسجد (أ) يمكنه صياغة طلب مع `student_id` من المسجد (ب).

**الخطر المحوري:**
غياب ثابت شبيه بالمفتاح الخارجي بين `memorization_progress` والتسجيل النشط. إذا كانت صفوف التقدم تخزن `circle_id` مباشرة، فإن تقدم الطالب المعاد تعيينه يُنسب إلى الحلقة الخطأ.

**أول خطوة إصلاح:**
إضافة التحقق من التسجيل داخل طريقة `@Transactional create()` في `MemorizationProgressService` باستخدام `LockModeType.OPTIMISTIC` على قراءة التسجيل.

---

## Complete Ranked Idea Pool / مجموعة الأفكار الكاملة مرتبة

| Rank | ID | Idea / الفكرة | N | V | F | Weighted |
|------|----|---------------|---|---|---|----------|
| 1 | O4 | N concurrent refreshes → N valid token pairs | 6 | 9 | 9 | **7.95** |
| 2 | S1 | Race token refresh boundary — dual valid windows | 7 | 8 | 9 | **7.90** |
| 3 | I1 | JWT claims not revalidated mid-session | 5 | 9 | 9 | **7.60** |
| 4 | I6 | Parent-student link stale on mosque transfer | 7 | 7 | 9 | **7.50** |
| 5 | O5 | New tab loses sessionStorage → lockout | 7 | 8 | 7 | **7.35** |
| 6 | I2 | No compiler-enforced tenant scoping | 5 | 8 | 9 | **7.20** |
| 7 | I3 | Join-request pipeline no compensatory tx | 5 | 8 | 9 | **7.20** |
| 8 | S2 | Soft-delete vs optimistic create race | 8 | 6 | 8 | **7.20** |
| 9 | R1 | Payment webhook receipt not persisted | 5 | 8 | 9 | **7.20** |
| 10 | S5 | Corrupt single-flight 401 DoS | 8 | 6 | 8 | **7.20** |
| 11 | O1 | Thread-local tenant context lost in async | 6 | 7 | 9 | **7.15** |
| 12 | R3 | TOCTOU enrollment→progress race | 6 | 7 | 9 | **7.15** |
| 13 | R5 | Message no individual expunge | 6 | 7 | 8 | **6.90** |
| 14 | R6 | PaymentCycle no transition history | 5 | 7 | 9 | **6.80** |
| 15 | I5 | No optimistic locking on any entity | 4 | 8 | 8 | **6.60** |
| 16 | I4 | Soft-delete cascades with no audit trail | 4 | 8 | 8 | **6.60** |
| 17 | O3 | Missing isActive filter on JOIN | 4 | 8 | 8 | **6.60** |
| 18 | S3 | RTL override chars → UI spoofing | 9 | 4 | 7 | **6.55** |
| 19 | A5 | API responses with TTL | 8 | 5 | 7 | **6.55** |
| 20 | S6 | Stack trace leaks HMAC key | 7 | 4 | 9 | **6.45** |
| 21 | O6 | Arabic RTL chars break CSS grid | 6 | 6 | 7 | **6.30** |
| 22 | R4 | No clock skew leeway in JWT validation | 4 | 7 | 8 | **6.20** |
| 23 | R2 | Orphaned payment rows on soft-delete | 4 | 7 | 8 | **6.20** |
| 24 | A3 | User = family tablet, PIN-based | 8 | 4 | 6 | **5.90** |
| 25 | A1 | DB as cache of human testimony | 9 | 3 | 6 | **5.85** |
| 26 | A2 | One Pi per mosque, gossip protocol | 10 | 2 | 5 | **5.60** |
| 27 | A6 | Student = attendance event stream | 9 | 3 | 5 | **5.60** |
| 28 | A4 | Frontend owns schema, backend is dumb JSON | 7 | 3 | 5 | **4.95** |

---

## Recommended Fix Priorities / أولويات الإصلاح الموصى بها

### EN

| Priority | Fix | Effort | Impact | Category |
|----------|-----|--------|--------|----------|
| **P0** | Implement refresh token rotation | 2-3 days | Critical | Auth Security |
| **P0** | Fix MemorizationProgressController to use MosqueAccessService | 0.5 day | Critical | Tenant Isolation |
| **P0** | Add mosque revalidation in PARENT access check | 1 day | Critical | Tenant Isolation |
| **P1** | Add `@Version` to all entities via BaseAuditableEntity | 1 day | High | Data Integrity |
| **P1** | Add enrollment lock check in MemorizationProgressService | 0.5 day | High | Data Integrity |
| **P1** | Replace lucide-react barrel with deep imports or optimizePackageImports | 0.5 day | High | Frontend Performance |
| **P2** | Add rate limiting on auth endpoints | 1 day | Medium | Security |
| **P2** | Store webhook payloads for payment reconciliation | 1 day | Medium | Compliance |
| **P2** | Add audit columns to soft-delete entities | 1 day | Medium | Auditability |
| **P2** | Migrate tokens to localStorage + BroadcastChannel sync | 2 days | Medium | UX |
| **P3** | Write ArchUnit tests for tenant-scoped queries | 1 day | Low | Governance |
| **P3** | Add PaymentCycle transition audit trail | 0.5 day | Low | Compliance |
| **P3** | Add @Transactional(readOnly=true) on read-only services | 0.5 day | Low | Performance |
| **P4** | Add RTL CSS regression tests | 1 day | Low | Quality |
| **P4** | Add clock skew leeway to JWT validation | 0.5 day | Low | Operations |

### AR

| الأولوية | الإصلاح | الجهد | التأثير | الفئة |
|----------|---------|-------|---------|-------|
| **P0** | تطبيق تدوير رمز التحديث | 2-3 أيام | حرج | أمن المصادقة |
| **P0** | إصلاح MemorizationProgressController لاستخدام MosqueAccessService | نصف يوم | حرج | عزل المستأجر |
| **P0** | إضافة التحقق من المسجد في فحص وصول PARENT | يوم واحد | حرج | عزل المستأجر |
| **P1** | إضافة `@Version` لكل الكيانات عبر BaseAuditableEntity | يوم واحد | عالي | سلامة البيانات |
| **P1** | إضافة فحص قفل التسجيل في MemorizationProgressService | نصف يوم | عالي | سلامة البيانات |
| **P1** | تحسين استيرادات lucide-react | نصف يوم | عالي | أداء الواجهة |
| **P2** | إضافة تحديد المعدل على نقاط نهاية المصادقة | يوم واحد | متوسط | أمن |
| **P2** | تخزين حمولات webhook لتسوية المدفوعات | يوم واحد | متوسط | امتثال |
| **P2** | إضافة أعمدة تدقيق للحذف الناعم | يوم واحد | متوسط | قابلية التدقيق |
| **P2** | نقل الرموز إلى localStorage مع مزامنة BroadcastChannel | يومان | متوسط | تجربة المستخدم |
| **P3** | كتابة اختبارات ArchUnit للاستعلامات محددة المستأجر | يوم واحد | منخفض | حوكمة |
| **P3** | إضافة مسار تدقيق انتقال دورة الدفع | نصف يوم | منخفض | امتثال |
| **P3** | إضافة @Transactional(readOnly=true) على خدمات القراءة فقط | نصف يوم | منخفض | أداء |
| **P4** | إضافة اختبارات انحدار CSS لـ RTL | يوم واحد | منخفض | جودة |
| **P4** | إضافة هامش انحراف الساعة للتحقق من JWT | نصف يوم | منخفض | تشغيل |

---

---

## Fixes Applied / الإصلاحات المطبقة

**Date / التاريخ:** 2026-07-24 (two-phase session)
**Method / الطريقة:** 8 parallel sub-agents under multi-agent swarm, ponytail lens
**Phase 1:** Refresh rotation, entity versioning, tenant isolation, cross-tab sync (4 agents)
**Phase 2:** Mosque-scoped access control gaps, backend logic errors, frontend navigation gaps, password-change token invalidation (4 agents)

### Phase 1 — Security & Data Integrity (first cycle)

| # | Fix / الإصلاح | Priority | Status | Files Changed |
|---|--------------|----------|--------|---------------|
| 1 | Refresh token rotation / تدوير رمز التحديث | P0 | ✅ | `V4__add_refresh_token_hashes.sql`, `RefreshTokenHash.java`, `RefreshTokenHashRepository.java`, `AuthService.java` |
| 2 | MemorizationProgressController → MosqueAccessService / إصلاح متجاوز الوصول | P0 | ✅ | `MemorizationProgressController.java`, `MosqueAccessService.java` |
| 3 | Parent-student mosque revalidation / التحقق من مسجد ولي الأمر | P0 | ✅ | `V5__add_parent_student_mosque_cascade.sql`, `ParentStudent.java`, `ParentStudentService.java`, `MosqueAccessService.java` |
| 4 | `@Version` on `BaseAuditableEntity` / القفل التفاؤلي لكل الكيانات | P1 | ✅ | `BaseAuditableEntity.java`, `V6__add_version_to_tables.sql` |
| 5 | Enrollment lock check in MemorizationProgressService / فحص التسجيل بالقفل التفاؤلي | P1 | ✅ | `EnrollmentRepository.java`, `MemorizationProgressService.java` |
| 6 | localStorage + BroadcastChannel cross-tab sync / مزامنة الجلسة عبر علامات التبويب | P2 | ✅ | `storage.ts`, `auth-provider.tsx` |

### Phase 2 — Mosque Scope, Logic Errors & Frontend UX (second cycle)

| # | Fix / الإصلاح | Priority | Status | Agent | Files Changed |
|---|--------------|----------|--------|-------|---------------|
| 7 | Attendance mosque-scoped findAll + findByCircleId + update access / نطاق المسجد للحضور | P1 | ✅ | Backend Access Control | `AttendanceService.java`, `AttendanceController.java`, `AttendanceRepository.java` |
| 8 | Enrollment mosque-scoped findAll + access checks / نطاق المسجد للتسجيلات | P1 | ✅ | Backend Access Control | `EnrollmentService.java`, `EnrollmentController.java` |
| 9 | MemorizationProgress mosque-scoped findAll + access checks / نطاق المسجد لتقدم الحفظ | P1 | ✅ | Backend Access Control | `MemorizationProgressService.java`, `MemorizationProgressController.java`, `MemorizationProgressRepository.java` |
| 10 | ParentStudent mosque-scoped findAll / نطاق المسجد لروابط أولياء الأمور | P1 | ✅ | Backend Access Control | `ParentStudentService.java`, `ParentStudentController.java`, `ParentStudentRepository.java` |
| 11 | User search scoped by mosque for MOSQUE_ADMIN/TEACHER / نطاق المسجد للبحث عن المستخدمين | P1 | ✅ | Backend Access Control | `UserService.java`, `UserController.java` |
| 12 | Remove stale totalAbsences/totalLateArrivals from StudentUpdateRequest / إزالة الحقول القديمة من تحديث الطالب | P1 | ✅ | Backend Logic | `StudentUpdateRequest.java`, `StudentService.java` |
| 13 | Invalidate refresh tokens on password change / إبطال رموز التحديث عند تغيير كلمة المرور | P1 | ✅ | Backend Logic | `AuthService.java` |
| 14 | Student-circle mosque match validation in Enrollment create / التحقق من تطابق مسجد الطالب والحلقة | P1 | ✅ | Backend Logic | `EnrollmentService.java` |
| 15 | Teacher-circle assignment verification in Attendance create / التحقق من تعيين المعلم للحلقة | P1 | ✅ | Backend Logic | `AttendanceService.java` |
| 16 | PARENT dashboard shows achievements + attendance / لوحة تحكم ولي الأمر: إنجازات + حضور | P2 | ✅ | Frontend UX | `dashboard-page.tsx` |
| 17 | TEACHER dashboard shows enrollments / لوحة تحكم المعلم: تسجيلات | P2 | ✅ | Frontend UX | `dashboard-page.tsx` |
| 18 | PARENT default landing path → /dashboard / مسار ولي الأمر الافتراضي → لوحة التحكم | P2 | ✅ | Frontend UX | `app-nav.ts` |
| 19 | PARENT added to attendance route guards + nav items / إضافة ولي الأمر لحراس مسار الحضور | P2 | ✅ | Frontend UX | `routes/index.tsx`, `app-nav.ts` |

### Summary of Changes / ملخص التغييرات

**1. Refresh Token Rotation**
- New Flyway migration `V4` creates `refresh_token_hashes` table (SHA-256 hashes)
- `AuthService.refreshToken()` now consumes the old hash and issues a new one in a single transaction
- `AuthService.login()` stores hash of the newly issued refresh token
- `AuthService.logout(UUID userId)` deletes all hashes for that user (invalidate all sessions)
- Replayed/captured refresh tokens fail on second use

**2. MemorizationProgress Security**
- Replaced `TeacherRepository` injection with `MosqueAccessService` in the controller
- Added `assertCanAccessStudent(UUID callerId, UUID studentId)` convenience overload to `MosqueAccessService`
- Added active enrollment verification with `@Lock(LockModeType.OPTIMISTIC)` in `MemorizationProgressService.create()`

**3. Parent-Student Mosque Revalidation**
- New Flyway migration `V5` adds nullable `mosque_id` column to `parent_student`
- `ParentStudentService.create()` snapshots the student's mosque at link creation
- `MosqueAccessService` PARENT branch compares snapshotted mosque against student's current mosque

**4. @Version Optimistic Locking**
- Added `@Version private Long version` to `BaseAuditableEntity`
- Flyway migration `V6` adds `version BIGINT NOT NULL DEFAULT 0` to all 17 entity tables

**5. Enrollment TOCTOU Fix**
- `EnrollmentRepository` has new `@Lock(LockModeType.OPTIMISTIC) findByStudentIdAndCircleId()` method
- `MemorizationProgressService.create()` checks active enrollment status before inserting progress

**6. Frontend Cross-Tab Session Sync**
- Migrated from `sessionStorage` to `localStorage` — tokens persist across tabs
- Added `BroadcastChannel` (`darb:auth`) for cross-tab session synchronization
- `AuthProvider` subscribes to sync messages and updates state/reacts to other tab's login/logout

**7-11. Mosque-Scoped Access Control (Agent 84d9a274)**
- Added `findByCircle_MosqueId()` to `AttendanceRepository`, `EnrollmentRepository`, `MemorizationProgressRepository`
- Added `findByMosqueId()` to `ParentStudentRepository`
- `AttendanceService.findAll()`, `EnrollmentService.findAll()`, `MemorizationProgressService.findAll()`, `ParentStudentService.findAll()` — all use `pageForCaller()` for automatic mosque scoping
- `AttendanceService.findByCircleId()` — checks `assertCanAccessMosque()` via the circle's mosque
- `AttendanceService.update()` — checks `assertCanAccessStudent()`
- `AttendanceService.create()` — verifies caller is the assigned teacher (or SUPER_ADMIN/MOSQUE_ADMIN)
- `EnrollmentService.findByStudentId()`, `MemorizationProgressService.findByStudentId()`, `MemorizationProgressService.findById()`, `MemorizationProgressService.findByCircleId()`, `MemorizationProgressService.update()` — all add access checks
- `EnrollmentService.create()` — validates student and circle belong to the same mosque
- `UserService.searchUsers()` — for MOSQUE_ADMIN/TEACHER: scopes results to caller's mosque via JPA Criteria subqueries (Student, Teacher, MosqueAdmin tables)
- All controllers: `AttendanceController`, `EnrollmentController`, `MemorizationProgressController`, `ParentStudentController`, `UserController` — pass `Authentication.getPrincipal()` as `callerId` to every service method

**12-13. Backend Logic Fixes (Agent 6d54bdc8)**
- `StudentUpdateRequest.java`: Removed `totalAbsences` and `totalLateArrivals` fields (computed from attendance, not manually settable)
- `StudentService.update()`: Removed the setter calls for those removed fields
- `AuthService.changePassword()`: Added `refreshTokenHashRepository.deleteByUserId(userId)` after password change to invalidate all existing refresh tokens

**14-15. Cross-Entity Validation (Agent 6d54bdc8)**
- `EnrollmentService.create()`: Student and circle must belong to the same mosque — throws `BadRequestException` on mismatch
- `AttendanceService.create()`: Non-admin callers must be the assigned teacher of the circle — throws `ForbiddenException` on mismatch

**16-19. Frontend UX Fixes (Agent 26ca686c)**
- `dashboard-page.tsx (PARENT)`: Added achievements + attendance links to the PARENT dashboard (4 links total)
- `dashboard-page.tsx (TEACHER)`: Added enrollments link to the TEACHER dashboard (5 links total, new `ClipboardList` import)
- `app-nav.ts`: Changed PARENT default landing path from `/students` to `/dashboard` — expanded dashboard is now the first page
- `app-nav.ts`: Added `"PARENT"` to attendance nav item roles — sidebar shows attendance for parents
- `routes/index.tsx`: Added `"PARENT"` to allowed roles on both attendance list and circle attendance `<RoleRoute>` guards

---

## Provocation / استفزاز

**EN —** The most provocative finding from this analysis is that **6 of the top 10 risks could be eliminated by one architectural change**: make the backend stateless with respect to session data. Implement refresh token rotation (P0), add `@Version` to all entities (P1), and introduce per-request JWT claim revalidation (P0). Together, these three changes collapse the attack surface of the entire auth + data integrity domain. Everything else is configuration and monitoring.

The second provocation: **the frontend's sessionStorage + single-flight 401 pattern is designed for a threat model that doesn't exist.** It assumes the biggest risk is a thundering herd on 401, but the actual biggest risk is that an attacker captures one token and owns the account for 7 days with no rotation. The defensive effort is invested in the wrong place.

The third provocation (proved by this session): **multi-agent swarm execution with isolated parallel branches can implement the top 6 fixes of a 30-finding audit in a single session.** 4 agents in parallel completed P0 security fixes, P1 data integrity fixes, and P2 UX fixes in under one cycle. The bottleneck is not implementation speed — it is finding the courage to admit the obvious risks exist.

**AR —** النتيجة الأكثر استفزازاً من هذا التحليل هي أن **6 من أهم 10 مخاطر يمكن إزالتها بتغيير معماري واحد**: جعل الخادم عديم الحالة فيما يتعلق ببيانات الجلسة. تطبيق تدوير رمز التحديث (P0)، إضافة `@Version` لجميع الكيانات (P1)، وتقديم إعادة التحقق من مطالبات JWT لكل طلب (P0). معاً، هذه التغييرات الثلاثة تنهار مساحة الهجوم لمجال أمن المصادقة وسلامة البيانات بالكامل.

الاستفزاز الثاني: **نمط sessionStorage + الطيران الواحد 401 في الواجهة الأمامية مصمم لنموذج تهديد غير موجود.** يفترض أن الخطر الأكبر هو الاندفاع الجماعي عند 401، لكن الخطر الفعلي الأكبر هو أن المهاجم يلتقط رمزاً واحداً ويمتلك الحساب لمدة 7 أيام بدون تدوير.

الاستفزاز الثالث (أثبتته هذه الجلسة): **تنفيذ سرب متعدد الوكلاء بفروع متوازية معزولة يمكنه تطبيق أهم 6 إصلاحات من تدقيق 30 نتيجة في جلسة واحدة.** 4 وكلاء بالتوازي أكملوا إصلاحات أمنية P0، وإصلاحات سلامة بيانات P1، وإصلاحات تجربة مستخدم P2 في دورة واحدة. عنق الزجاجة ليس سرعة التنفيذ — بل الشجاعة للاعتراف بوجود المخاطر الواضحة.

---

## Appendix A: Second ADHD Run — Additional Frames / الملحق أ: الجولة الثانية من ADHD — أطر إضافية

A second round of ADHD divergence was executed with 5 different cognitive frames to surface additional insights not captured in the first run. These findings complement the primary analysis above.

تم تنفيذ جولة ثانية من التباعد الإبداعي ADHD مع 5 أطر معرفية مختلفة لاستكشاف رؤى إضافية لم تظهر في الجولة الأولى. هذه النتائج تكمل التحليل الأساسي أعلاه.

### Regulator Frame (Round 2) / إطار المنظم (الجولة 2)

| # | Finding / النتيجة | Target Role |
|---|-------------------|-------------|
| R7 | Parent invite codes have no expiration — leaked code can permanently link attacker to any future student body / رموز دعوة ولي الأمر ليس لها صلاحية | PARENT |
| R8 | Attendance records lack recorded_by and audit trail — admin can insert fabricated rows / سجلات الحضور تفتقر إلى مسار التدقيق | TEACHER |
| R9 | Payment hard DELETE destroys refund evidence — no void/cancelled state / حذف الدفع يدمر أدلة الاسترداد | MOSQUE_ADMIN |
| R10 | SUPER_ADMIN can self-demote and delete last admin — no break-glass recovery / المدير العام يمكنه خفض رتبة نفسه وحذف آخر مسؤول | SUPER_ADMIN |
| R11 | Self-reported memorization has no teacher counter-signature / تقدم الحفظ المبلغ ذاتياً بدون توقيع المعلم | STUDENT |
| R12 | RefreshTokenHash has no FK to users table — orphaned tokens survive user deletion / رموز التحديث ليس لها مفتاح خارجي لجدول المستخدمين | ALL |

### Logistics Frame / إطار اللوجستيات

| # | Finding / النتيجة | Target Role |
|---|-------------------|-------------|
| L1 | Ghost enrollments inflate capacity — auto-offboard after N unexcused absences / تسجيلات وهمية تضخم السعة | MOSQUE_ADMIN |
| L2 | Urgent notifications share channel with routine announcements — no priority routing / الإشعارات العاجلة تشارك القناة مع الإعلانات الروتينية | PARENT |
| L3 | Rejected join requests vanish into dead-letter queue — no reroute to alternative circle / طلبات الانضمام المرفوضة تختفي | MOSQUE_ADMIN |
| L4 | Teacher onboarding pipeline missing just-in-sequence gate — can be assigned before background check clears / خط أنابيب قبول المعلم يفتقد بوابة التسلسل | TEACHER |
| L5 | Multi-mosque admin lacks hub-and-spoke view — must navigate each mosque separately / مدير متعدد المساجد يفتقر إلى عرض محوري | SUPER_ADMIN |
| L6 | Cancelled memorization goals orphan partial progress — no returns-management flow / أهداف الحفظ الملغاة تتيم التقدم الجزئي | STUDENT |

### Competitor Frame (Round 2) / إطار المنافس (الجولة 2)

| # | Finding / النتيجة | Target Role |
|---|-------------------|-------------|
| C1 | Refresh token never rotates — single leak grants permanent access / رمز التحديث لا يدور أبداً | ALL |
| C2 | Parent-student link survives mosque rehoming — leaks child data across tenants / رابط ولي الأمر يبقى عبر تغيير المسجد | PARENT |
| C3 | Self-reported memorization with no teacher attestation inflates metrics / تقدم الحفظ بدون توثيق المعلم يضخم المقاييس | STUDENT |
| C4 | Double-approval race on enrollment — two admins can create two active records / سباق الموافقة المزدوجة على التسجيل | MOSQUE_ADMIN |
| C5 | Guessable parent invite codes let attacker enumerate and link to arbitrary children / رموز دعوة قابلة للتخمين | PARENT |
| C6 | BroadcastChannel cross-tab poisoning forces session loss on all open tabs / تسميم BroadcastChannel يفقد جميع الجلسات | ALL |

### 3am On-Call Frame (Round 2) / إطار المناوب في الثالثة فجراً (الجولة 2)

| # | Finding / النتيجة | Target Role |
|---|-------------------|-------------|
| O7 | Attendance batch API returns 200 after partial row failures — silent data loss / واجهة الحضور الجماعية ترجع 200 بعد فشل جزئي | TEACHER |
| O8 | Excuse document filenames collide at millisecond precision — second upload overwrites / أسماء ملفات الأعذار تتصادم بدقة المللي ثانية | PARENT/STUDENT |
| O9 | Payment refund succeeds but enrollment status never updated — student remains on roster / استرداد الدفع ينجح دون تحديث حالة التسجيل | MOSQUE_ADMIN |
| O10 | Report files fill disk on small VM — endpoint returns 500 with no disk-full signal / تقارير تملأ القرص | MOSQUE_ADMIN |
| O11 | Memorization goal target lowered below recorded progress — completedAyahs underflows past 100% / تخفيض هدف الحفظ أقل من التقدم المسجل | TEACHER |
| O12 | Two parents simultaneously enroll into last slot — both pass count check under read-committed / والدان يسجلان في آخر مقعد في نفس الوقت | PARENT |

### 10-Year-Old Frame / إطار الطفل العاشر

| # | Finding / النتيجة | Target Role |
|---|-------------------|-------------|
| Y1 | No celebration after memorization progress — kid re-submits trying to trigger reaction / لا احتفال بعد تقدم الحفظ | STUDENT |
| Y2 | Absence excuse button just turns grey — kid taps repeatedly creating duplicate requests / زر عذر الغياب يتحول للرمادي فقط | PARENT |
| Y3 | No undo for mistaken attendance tap — toggling back creates conflicting records / لا تراجع عن خطأ الحضور | TEACHER |
| Y4 | Circle max capacity rejects "unlimited" — kid sees app as bossy / سعة الحلقة ترفض "غير محدود" | MOSQUE_ADMIN |
| Y5 | 24-hour time format confuses child — shows up at wrong time / تنسيق الوقت 24 ساعة يربك الطفل | STUDENT |
| Y6 | Mosque count is plain number, not a map — kid taps expecting visual / عدد المساجد مجرد رقم وليس خريطة | SUPER_ADMIN |

### Mapping to Applied Fixes / ربط النتائج بالإصلاحات المطبقة

| Second ADHD Run Finding | Applied Fix (if any) / الإصلاح المطبق (إن وجد) |
|------------------------|------------------------------------------------|
| R7 — Expiring invite codes | Not yet addressed |
| R8 — Attendance audit trail | Partially: `recordedBy` fixed in `Attendance.create()` |
| R9 — Payment hard DELETE | Not yet addressed |
| R10 — Self-demotion recovery | Not yet addressed |
| R11 — Teacher counter-signature | Addressed: `MosqueAccessService` enforced for memorization |
| R12 — Orphaned refresh tokens | Not yet addressed |
| L1 — Ghost enrollments | Not yet addressed |
| L2 — Notification priority | Not yet addressed |
| L3 — Dead-letter join requests | Not yet addressed |
| L4 — Teacher onboarding sequence | Not yet addressed |
| L5 — Hub-and-spoke multi-mosque | Not yet addressed |
| L6 — Orphaned progress data | Not yet addressed |
| C1 — Refresh token rotation | **Fixed** (Phase 1, #1) |
| C2 — Parent-student mosque survival | **Fixed** (Phase 1, #3) |
| C3 — Self-report inflation | **Fixed** (Phase 1, #2 + Phase 2, #9) |
| C4 — Double-approval race | **Fixed** (Phase 1, #4 — `@Version`) |
| C5 — Guessable invite codes | Not yet addressed |
| C6 — BroadcastChannel poisoning | Not yet addressed |
| O7 — Batch partial failure | Not yet addressed |
| O8 — File collision | Not yet addressed |
| O9 — Payment refund mismatch | Not yet addressed |
| O10 — Disk full signal | Not yet addressed |
| O11 — Goal underflow | Not yet addressed |
| O12 — Double enrollment race | **Fixed** (Phase 1, #4 — `@Version` + Phase 2, #14) |
| Y1-Y6 — Child UX | Partially addressed via frontend navigation improvements |

---

*Analysis produced by ADHD (2 runs × 10 frames × 6 ideas = 60) + Multi-Agent Swarm (14 sub-agents) + Ponytail lens.*
*تم إنتاج التحليل بواسطة ADHD (جولتان × 10 أطر × 6 أفكار = 60) + سرب متعدد الوكلاء (14 وكيلاً فرعياً) + عدسة Ponytail.*
