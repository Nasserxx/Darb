# Extending the frontend

Guide for adding features, routes, locales, and protected pages in line with the existing auth slice.

## Add a feature slice

1. Create `src/features/<feature>/` with:
   - `api/` — functions that call `apiFetch` from `src/lib/api-client.ts`
   - `schemas/` — Zod schemas and `*RequestBody` types
   - `components/` — feature UI
   - `hooks/` — optional data hooks
   - `types/index.ts` — feature types
   - `index.ts` — **public exports only** (barrel)

2. Keep pages thin: `src/pages/<area>/<name>-page.tsx` should import from `@/features/<feature>`.

3. Do not import another feature’s internals; share via `lib/` or `components/` if truly cross-cutting.

Example barrel (`index.ts`):

```ts
export { MyWidget } from "./components/my-widget.tsx";
export type { MyDto } from "./types/index.ts";
```

## Add routes

Edit `src/routes/index.tsx`:

- All user-facing routes live under `path: "/:locale"` + `LocaleLayout`
- **Guest-only** (login-style): wrap with `<GuestRoute>`
- **Authenticated:** wrap with `<ProtectedRoute>`

```tsx
{
  path: "my-feature",
  element: (
    <ProtectedRoute>
      <MyFeaturePage />
    </ProtectedRoute>
  ),
},
```

Root `/` redirects to `/en/login`. Preserve the locale segment when linking:

```tsx
const { locale } = useParams<{ locale: string }>();
const prefix = locale ?? DEFAULT_LOCALE;
<Link to={`/${prefix}/my-feature`}>...</Link>
```

## Add a locale

1. Add `src/i18n/locales/<code>.json` with the same structure as `en.json` (`common` and `auth` objects at minimum for auth flows).

2. Register in `src/i18n/index.ts`:
   - Append to `SUPPORTED_LOCALES`
   - Import bundle into `resources`
   - If RTL: add locale to `RTL_LOCALES`

3. Add labels in `src/components/locale-switcher.tsx` if it lists locales explicitly.

4. Route URLs automatically accept the new code: `/{code}/login`.

## Protected page checklist

Use this when adding a page that requires a logged-in user:

- [ ] Page component under `src/pages/app/` (or consistent area folder)
- [ ] Route registered under `/:locale/...` inside `ProtectedRoute` in `src/routes/index.tsx`
- [ ] API calls use `apiFetch(path, { auth: true })` so 401 triggers refresh-or-logout
- [ ] Copy/links use `useParams().locale` (or `DEFAULT_LOCALE`) for path prefix
- [ ] User-visible strings use `useTranslation` with the right namespace (`common` / `auth` / new namespace)
- [ ] Errors: surface `AuthActionResult`-style results or catch `ApiError` and map `message` / `fieldErrors` like auth forms
- [ ] Optional: gate UI by `useAuth().user.role` using `normalizeRole` from `@/features/auth` if role-specific

## Auth-specific notes

- Session is global via `AuthProvider` in `App.tsx`; do not nest second providers
- For forms, follow `register-form.tsx` / `login-form.tsx`: Zod + `react-hook-form` + `toast` + `applyFieldErrors`
- New authenticated endpoints: add to `features/auth/api/auth-api.ts` or a new feature `api/` module, not inline `fetch` in components

See [auth feature README](../src/features/auth/README.md) for endpoint and export details.
