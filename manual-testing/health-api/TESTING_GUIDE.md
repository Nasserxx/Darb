# Health API — Testing Guide

Step-by-step manual test plan for **`/api/v1/health`** (service liveness),
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

- Backend running (port 8089).
- No auth required — the endpoint is in `SecurityConfig.PUBLIC_ENDPOINTS`
  (`/api/v1/health`), so any request (even without a token) is allowed.
- DBeaver not needed: this resource performs **no DB access**.

## 1. Endpoint inventory (press everything)

| # | Method | Endpoint | Roles allowed |
| --- | --- | --- | --- |
| 1 | GET | `/api/v1/health` | public (no token required) |

`HealthController` (`@RequestMapping("/api/v1")` + `@GetMapping("/health")`)
returns a JSON body `{"status":"UP"}`. Note `SecurityConfig` also lists
`/api/health` as public, but **no controller maps it** — expect 404.

## 2. Step-by-step

**Step 1 — GET `/api/v1/health`:** no token → **200** with `{"status":"UP"}`.
Valid token (any role) → **200**. Garbage token → **200** (the filter ignores
invalid tokens and the endpoint is permitAll). Missing/unmapped variant
`/api/health` → **404** (no handler).

Because the endpoint is permitAll and takes no parameters there are no
401/403/400/404-by-input cases beyond the unmapped path above.

## 3. DB changes (DBeaver)

None. `HealthController.health()` only logs at DEBUG and returns a `Map`; no
repository is touched.

## 4. Flow — normal and logical

- Liveness: `GET /api/v1/health` → `200 {"status":"UP"}` — use it to confirm
  the app is up before running the other API suites.
- Note this is liveness only (no DB/readiness probe). If the DB is down the
  health check still reports UP.

## 5. Role UI (frontend)

Not applicable — no user-facing UI surface for a health probe. (Ops tooling,
not a feature.)

## 6. Results documentation

Run `.\run-tests.ps1` → prints the table and writes `results.csv`. Manual
tracking is usually unnecessary for a single public endpoint.

## Known gaps to flag

- (a) `SecurityConfig.PUBLIC_ENDPOINTS` references `/api/health` but
  `HealthController` only maps `/api/v1/health` — `/api/health` is allowed
  through auth yet returns 404 (no handler). Either a leftover config entry or
  an expected infra probe; worth confirming which.
- (b) Liveness only — no readiness/DB connectivity indicator.
