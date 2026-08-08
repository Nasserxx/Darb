# Frontend implementation

This document describes how the Darb auth frontend is structured and how cross-cutting concerns work.

## Vertical slice architecture

Features are organized as **vertical slices** under `src/features/<name>/`. Each slice owns its API calls, Zod schemas, UI components, hooks, and types. Thin **pages** in `src/pages/` compose slice components; **routes** wire pages to URLs.

```
src/
├── features/auth/     # Auth slice (API, session, forms, context)
├── pages/             # Route-level page shells
├── routes/            # React Router config + guards
├── lib/               # Shared api-client (used by auth today)
├── components/        # Shared UI (shadcn-style) + locale-switcher
└── i18n/              # i18next setup and locale JSON
```

**Dependency direction:** pages → features → `lib/`. The auth feature does not import from other feature slices (none exist yet). Shared HTTP logic lives in `lib/api-client.ts` and is imported by `features/auth/api/auth-api.ts`.

## JWT lifecycle

### Storage (`features/auth/session/storage.ts`)

Tokens and user profile fields are stored in **`sessionStorage`** (tab-scoped):

| Key | Content |
| --- | --- |
| `darb.accessToken` | JWT access token |
| `darb.refreshToken` | Refresh token |
| `darb.user` | JSON: `tokenType`, `expiresIn`, `userId`, `fullName`, `role`, `email` |

`setSessionFromAuthResponse` writes all three after login or refresh. `clearSession` removes them on logout or failed refresh.

### Login

1. `POST /api/v1/auth/login` via `auth-api.login`
2. Response wrapped in `ApiResponse<AuthResponse>`; on success, session is persisted and `UserSession` returned
3. `AuthProvider` sets React state, schedules proactive refresh

### Proactive refresh (`AuthProvider`)

- Default access token lifetime assumption: **900s** (15 min), overridable via `expiresIn` from API
- A timer fires **60 seconds before** expiry and calls `refresh()`
- On failure: timer cleared, session cleared, user set to `null`

### Reactive refresh (`lib/api-client.ts`)

- `apiFetch(..., { auth: true })` attaches `Authorization: Bearer <accessToken>`
- On **401**, a **single-flight** `POST /api/v1/auth/refresh` retries the original request once
- If refresh fails, session is cleared and `ApiError(401, "Session expired")` is thrown

### Logout

`AuthProvider.logout` clears the refresh timer, `clearSession()`, and user state. No server logout endpoint is called yet.

## Folder map

| Path | Role |
| --- | --- |
| `src/main.tsx` | Bootstraps i18n, renders `App` |
| `src/App.tsx` | `AuthProvider`, `RouterProvider`, Sonner toasts |
| `src/routes/index.tsx` | Browser routes under `/:locale/...` |
| `src/routes/locale-layout.tsx` | Validates locale, `persistLocale`, `<Outlet />` |
| `src/routes/guest-route.tsx` | Redirects authenticated users away from login/register |
| `src/routes/protected-route.tsx` | Redirects guests to login |
| `src/lib/api-client.ts` | `apiFetch`, `ApiError`, refresh-on-401 |
| `src/features/auth/context/auth-provider.tsx` | Session state + `login` / `register` / `changePassword` |
| `src/features/auth/api/auth-api.ts` | Auth HTTP functions |
| `src/features/auth/components/*` | Forms + `AuthShell` layout |
| `src/features/auth/schemas/*` | Zod + request body mappers |
| `src/features/auth/utils/form-errors.ts` | Maps API messages to i18n + RHF field errors |
| `src/pages/auth/*` | Login / register pages |
| `src/pages/app/*` | Dashboard, change-password (protected) |
| `src/i18n/index.ts` | i18next init, RTL, locale persistence |
| `src/i18n/locales/{en,ar,de}.json` | `common` + `auth` namespaces |
| `src/components/ui/*` | Button, Input, Field, Select, Card, Spinner |
| `src/components/locale-switcher.tsx` | Switches `en` / `ar` / `de` |

## `ApiResponse` and errors

Backend responses use `ApiResponse<T>`:

```ts
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}
```

### HTTP layer (`apiFetch`)

- Non-OK status → `ApiError` with `status`, `message` (from `body.message` when present), and `body`
- Login/refresh paths in `auth-api.ts` also check `success` and `data` on 200 responses

### Auth actions (`AuthProvider`)

Returns `AuthActionResult`:

- `{ ok: true }` on success
- `{ ok: false, message, fieldErrors? }` on failure

`fieldErrors` are extracted from `ApiResponse` validation payloads where `data` is a `Record<string, string>` (see `GlobalExceptionHandler.handleValidation` on the backend). Forms apply them via `applyFieldErrors` and translate messages with `translateAuthApiMessage` (`auth:errors.api.<message>` keys in locale files).

### Register endpoint

`POST /api/v1/auth/register` returns **201 with no body**. `register()` uses `apiFetch<void>` and does not expect an `ApiResponse` wrapper.

## i18n and RTL

- **Libraries:** `i18next`, `react-i18next`
- **Locales:** `en` (default), `ar`, `de` — `SUPPORTED_LOCALES` in `src/i18n/index.ts`
- **Namespaces:** `common`, `auth` (forms, roles, API error strings)
- **Persistence:** `localStorage` key `darb-locale` (survives reload; separate from JWT session)
- **RTL:** `ar` sets `document.documentElement.dir = 'rtl'` via `persistLocale` / initial init
- **Routes:** First segment is locale: `/:locale/login`, `/:locale/dashboard`, etc. Invalid locale → redirect to `/{DEFAULT_LOCALE}/...`
- **Roles:** API returns uppercase enums (`STUDENT`, …); `roleI18nKey` / `translateRole` map to `auth:roles.*`

## `sessionStorage` tradeoff

| Choice | Benefit | Cost |
| --- | --- | --- |
| `sessionStorage` for tokens | Cleared when tab closes; not shared across tabs; slightly smaller XSS blast radius than `localStorage` for tokens | New tab = logged out; not suitable for “remember me” without moving to `localStorage` or httpOnly cookies |
| `localStorage` for locale only | Language preference survives new tabs | Locale and auth storage policies differ intentionally |

For production hardening, consider **httpOnly, Secure, SameSite** cookies issued by the backend instead of JS-readable tokens; that would replace `storage.ts` and parts of `api-client.ts`.

## Environment variables

| Variable | Where | Purpose |
| --- | --- | --- |
| `VITE_API_URL` | `frontend/.env*` | API origin for `apiFetch` (Vite exposes only `VITE_*` to client) |
| `CORS_ALLOWED_ORIGINS` | Repo root `.env` (backend) | Allowed browser origins for `/api/**` |

Backend defaults: API `8089`, CORS `http://localhost:3000`. Frontend defaults: `VITE_API_URL` → `http://localhost:8089`, Vite dev port `3000`.
