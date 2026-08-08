# AGENTS.md — Darb

Persistent core instructions for AI coding agents (opencode) working on this
repo. This file is loaded at the start of every session. For how opencode
should behave, read `.opencode/SYSTEM.md` (auto-loaded). For how to use
opencode itself, see `.opencode/CHEATSHEET.md`.

## Project at a glance

**Darb** — Quran memorization & mosque management platform.

- **Backend** — Spring Boot 4.0.5, Java 25, Maven wrapper, PostgreSQL,
  Flyway, JWT, Testcontainers. Package root `com.darb`, layered.
- **Frontend** — React 19 + TypeScript + Vite SPA, Zod, react-hook-form,
  i18n (`en`/`ar`/`de`, RTL), locale-prefixed routes.
- **e2e** — Playwright suite under `e2e/`.

## Golden rules

1. Read before you change; follow existing conventions; no scope creep.
2. Never change pinned toolchain versions (Java/Spring/Maven — see README table).
3. No secrets in code, commits, or logs.
4. Verify with CI-equivalent commands before calling work done.
5. **Planning lives in opencode's own docs** — check `.opencode/docs/specs/`
   and `.opencode/docs/plans/` first. If a spec/plan covers the task, follow
   it and do NOT re-plan. Only plan when nothing exists, and save new plans
   under `.opencode/docs/`. The top-level `docs/` belongs to another dev.

## Quick commands (Windows / PowerShell)

| Task | Command |
| --- | --- |
| Backend tests | `just backend-test` (needs Docker/Testcontainers) |
| Backend dev | `just backend-run` |
| Frontend lint | `cd frontend; npm run lint` |
| Frontend build | `cd frontend; npm run build` |
| Frontend dev | `just frontend-run` (port 3000) |
| e2e | `cd e2e; npm run test` (backend must be running) |
| Infra up/down/restart | `just infra-up` / `just infra-down` / `just infra-restart` |
| Setup envs + JWT_SECRET | `just setup-env` |

## The authoritative behavior spec

`.opencode/SYSTEM.md` — the general operating manual for all cases (planning,
execution, surgical modification), plus conventions for backend layers,
frontend feature slices, routing, i18n, and the definition of done. Follow it.

- Tooling for opencode: config `.opencode/opencode.json`, system prompt
  `.opencode/SYSTEM.md`, cheat sheet `.opencode/CHEATSHEET.md`, ignore list
  `.opencodeignore`, skills `.opencode/skill/` (plus `.agents/skills/`),
  agents `.opencode/agent/`, commands `.opencode/command/`, and opencode's own
  planning docs under `.opencode/docs/`.
