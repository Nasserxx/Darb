# Darb frontend

React 19 + TypeScript + Vite SPA for Darb authentication (register, login, JWT session, change password) with locale-prefixed routes and RTL support.

## Prerequisites

- **Node.js** 20+ (LTS recommended)
- **npm** 10+
- **Backend** running at `http://localhost:8089` (see repo root [README](../README.md) and `backend/`)

## Quick start

```bash
cd frontend
npm install
npm run dev
```

The dev server listens on **http://localhost:3000** (`server.port` in `vite.config.ts`).

Open a locale-prefixed auth route, for example:

- http://localhost:3000/en/login
- http://localhost:3000/en/register

## Environment variables

| Variable | Required | Default | Description |
| --- | --- | --- | --- |
| `VITE_API_URL` | No | `http://localhost:8089` | Spring Boot API base URL (no trailing slash). Read at build/dev time via `import.meta.env.VITE_API_URL`. |

Create `frontend/.env.local` (gitignored) for local overrides:

```env
VITE_API_URL=http://localhost:8089
```

Copy from the repo root [`.env.example`](../.env.example) when running backend + frontend together.

## CORS

Browser requests from `http://localhost:3000` to the API require the backend to allow that origin. Spring reads `CORS_ALLOWED_ORIGINS` (comma-separated); default is `http://localhost:3000` in `CorsConfig`. If you change the Vite port or host, update both `CORS_ALLOWED_ORIGINS` and `VITE_API_URL` so they stay aligned.

## Scripts

| Command | Description |
| --- | --- |
| `npm run dev` | Vite dev server (port 3000) |
| `npm run build` | Typecheck + production build → `dist/` |
| `npm run preview` | Serve production build locally |
| `npm run lint` | ESLint |

## Documentation

- [Implementation guide](docs/IMPLEMENTATION.md) — architecture, JWT lifecycle, folder map, errors, i18n
- [Extending the app](docs/EXTENDING.md) — new feature slices, routes, locales, protected pages
- [Auth feature](src/features/auth/README.md) — API endpoints and public exports

## Path alias

`@/` resolves to `src/` (see `vite.config.ts` and `tsconfig.app.json`).
