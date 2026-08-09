# Auth API — Testing Guide

Step-by-step manual test plan for **`/api/v1/auth`** (Registration, login, token
refresh, password change), mapped to the project's testing considerations:

1. Press everything
2. Test logic
3. DB changes
4. Flow — normal and logical
5. Role UI
6. Results documentation

Automated matrix: run `.\run-tests.ps1` in this folder (prints results table +
`results.csv`).

> **Logout note:** the task brief mentions a logout endpoint. From reading the
> code, `AuthService.logout(UUID)` exists (`AuthService.java:152`, deletes all
> `refresh_token_hashes` for the user) but **no controller maps it** — there is
> no `POST /api/v1/auth/logout` in `AuthController.java`. A request to that
> path returns 404. This is a gap, see "Known gaps".

## 0. Preconditions

- Backend running (port 8089) with the 5 seed users.
- Tokens: login as SUPER_ADMIN, MOSQUE_ADMIN, TEACHER, STUDENT, PARENT.
- DBeaver open on the `darb` DB.
- `run-tests.ps1` registers a **throwaway user** (unique email per run,
  e.g. `auth.test.<suffix>@darb.app` / `Auth12345!`) and uses it for password
  change — never change a seed user's password.

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | POST | `/api/v1/auth/register` | public (no token) |
| 2 | POST | `/api/v1/auth/login` | public (no token) |
| 3 | POST | `/api/v1/auth/refresh` | public (no token) — body: `refreshToken` |
| 4 | POST | `/api/v1/auth/change-password` | any authenticated (access token in `Authorization: Bearer`) |

Security note: `SecurityConfig.PUBLIC_ENDPOINTS` permits **all** of
`/api/v1/auth/**`, so register/login/refresh need no token, and
`change-password` is also permitAll at the filter level — it only works
because the handler requires a real access token.

## 2. Step-by-step

**Step 1 — POST `/register`:** valid body (`fullName`, `email`, `password`
min 8, optional `role`/`phone`/`gender`/`dateOfBirth`) → **201** (no body).
Self-registration allows `student|teacher|parent|mosque_admin` only; role
`SUPER_ADMIN` → **400**. Missing `fullName` or short password → **400**.
Duplicate email/phone → **409**. No token needed.

**Step 2 — POST `/login`:** valid seed credentials → **200** with
`data.accessToken`, `data.refreshToken`, `data.userId`, `data.role`, `data.expiresIn`
(900s). Wrong password → **401**. Empty/malformed body → **400**.
Deactivated account (login → 401 `"Account is deactivated"`) can be verified
manually after `user-api` deactivates `test.user@darb.app`.

**Step 3 — POST `/refresh`:** send a valid **refresh** token (from login)
→ **200** with a *new* token pair (old refresh token is rotated/consumed).
Garbage token → **401**. Using an **access** token instead of a refresh token
→ **401** (`token_type` must be `refresh`). Empty body → **400**.

**Step 4 — POST `/change-password`** (`Authorization: Bearer <access>`):
wrong `currentPassword` → **400** (no DB change). `newPassword` < 8 chars →
**400**. Missing fields → **400**. Correct current + new → **200**; after that
all refresh tokens for the user are deleted (`AuthService.changePassword` →
`refreshTokenHashRepository.deleteByUserId`), so the old refresh token → **401**
and login with the old password → **401**. No token / garbage token → **500**
(see gap (b) — the route is permitAll so the anonymous request reaches the
handler and fails with a cast/NPE, not 401).

**Step 5 — logout (gap):** `POST /api/v1/auth/logout` with any token → **404**
endpoint not implemented.

## 3. DB changes (DBeaver)

Tables: `users`, `refresh_token_hashes`.

- `register` → new `users` row (`is_active=true`, `last_login=null`,
  `created_at`/`updated_at` set, `version=0`).
- `login` → `users.last_login` set to now, `updated_at` bumped, `version`
  bumped; **one new `refresh_token_hashes` row** (`token_hash` = SHA-256 of
  the refresh token, `created_at`, `version`).
- `refresh` → old `refresh_token_hashes` row **deleted**, one new row inserted
  (rotation). Token hashes are only ever stored as SHA-256 — never raw tokens.
- `change-password` → `users.password_hash` replaced (BCrypt), `updated_at`
  bumped, `version` bumped, and **all** `refresh_token_hashes` for the user are
  deleted.
- No endpoint triggers `refresh_token_hashes` deletion other than
  `change-password` (logout is unimplemented).

## 4. Flow — normal and logical

- Happy path: register → login → use access token → refresh → login again.
- Password change: login → change-password → old password rejected (401), old
  refresh token rejected (401), new password works.
- Token hygiene: access token has no `role` claim mismatch with DB; refresh
  token never accepted as a bearer access token (JWT filter requires
  `token_type=access`).
- Rotation: a refresh token is single-use (second refresh with same token →
  401).

## 5. Role UI (frontend)

No role-specific UI surfaces: register/login/refresh are guest pages; password
change is a form available to any authenticated user (LocaleLayout guest/auth
wrappers). Illegal CTAs: none specific to auth.

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. For
manual-only cases use:

| # | Endpoint | Role | Input | Expected | Actual | DB after | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | POST /auth/login | none | deactivated user | 401 | | | ✅/❌ |

## Known gaps to flag

- (a) **No logout endpoint.** `AuthService.logout(UUID)` exists
  (`AuthService.java:152`) but is not exposed by any controller — refresh
  tokens can only be invalidated via password change. `POST /auth/logout` → 404.
- (b) **change-password is not auth-enforced at the filter level.** Because
  `/api/v1/auth/**` is permitAll, a request without a token reaches the handler
  with an anonymous principal and blows up (expected 500, not the documented
  401). Should be protected at the route level, not just in code.
- (c) No self-service password reset / forgot-password flow (out of scope,
  but worth flagging).
