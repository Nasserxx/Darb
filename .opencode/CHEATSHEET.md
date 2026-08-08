# opencode — Cheat Sheet

How to get the most out of opencode when working on **Darb**. Covers the TUI,
slash commands, modes, agents, and config. Keep it handy.

## 1. The interface (TUI)

- **Type to chat** with the current agent. **Enter** sends, **Esc** stops.
- **Tab** cycles modes: `build` (default, edits files), `plan` (research only,
  no edits), `agent` (switch agents).
- **`Ctrl+K`** — open file search / jump to a file.
- **`@`** in the input — mention a file, a skill, an agent, or a **reference**
  to attach it to the conversation.
- **`/`** — open the slash-command menu (see §2).
- **Scroll** with mouse / arrows; `Ctrl+C` cancels a running task.

## 2. Slash commands

| Command | What it does |
| --- | --- |
| `/init` | Set up / update opencode config for this project |
| `/plan` | Switch to plan mode (read-only research) |
| `/build` | Switch to build mode (make changes) |
| `/new` | Start a fresh session |
| `/share` | Create a shareable link of the session |
| `/undo` | Roll back the last change |
| `/models` | Switch model |
| `/mcp` | Manage MCP servers |
| `/opencode` | Open opencode config |
| `/todos` | View / manage task list |
| `/yolo` | Allow everything without asking (use with care) |

Project commands you define live in `.opencode/command/*.md` and show up in
this menu (e.g. add `/deploy`, `/test-backend`, `/lint-frontend`).

## 3. Modes & agents

- **build** — default. Can read/edit files, run commands. Use for implementing.
- **plan** — read-only. Use for exploring, designing, or reviewing before
  touching code. It never edits.
- **agent** — invoke subagents. opencode ships with `build`, `plan`, `general`,
  `explore`. You can add your own in `.opencode/agent/*.md` (e.g. a
  "backend-reviewer" or "frontend-ui" agent).

For big or ambiguous Darb work: **start in plan mode, then build.**

## 4. Skills

Skills are procedural instructions opencode can load on demand
(`@skill` or auto-triggered). Project skills live in `.opencode/skill/*/SKILL.md`.

Available for Darb (bundled under `.agents/skills/`):
`improve` (audit/roadmap), `graphify` (knowledge-graph queries over the
codebase), `frontend-design`, `performance`, `seo-audit`, `ponytail`
(minimal-solution style), `vercel-react-best-practices`, and more.

## 5. References

Make folders outside the project (or other repos) available as context with
`@alias` by adding them to `opencode.json`:

```json
"references": {
  "specs": { "path": "../shared-specs", "description": "Shared product specs" },
  "sdk":   { "repository": "owner/sdk", "branch": "main", "description": "..." }
}
```

## 6. Config quick reference (`opencode.json`)

- `$schema` → `https://opencode.ai/config.json` (editor autocomplete/validation).
- `instructions` → list of markdown files always loaded (e.g. `AGENTS.md`,
  `.opencode/SYSTEM.md`).
- `permission` → per-tool allow/ask/deny rules (`edit`, `bash`, `read`,
  `external_directory`, ...). Last matching rule wins.
- `model` / `small_model` → `provider/model-id`, e.g. `"anthropic/claude-sonnet-4-6"`.
- `agent`, `command`, `skill`, `mcp`, `plugin` → all configurable here.
- Reload needed: **opencode reads config on startup only** — after editing
  `opencode.json` or adding commands/skills/agents, quit and restart opencode.

## 7. Working with Darb — good habits

1. **Let opencode read the context first.** Point it at `AGENTS.md` +
   `.opencode/SYSTEM.md` (already auto-loaded) so it follows repo conventions.
2. **Use `@` to attach** the exact files you want worked on — faster and more
   reliable than describing paths.
3. **Verify with the same commands CI uses.** Backend: `just backend-test`.
   Frontend: `cd frontend; npm run lint; npm run build`.
4. **Plan mode for design/spec work**, then switch to build to implement.
5. **Keep sessions focused** — one feature/task per session; start a new one
   with `/new` for unrelated work.
6. **Share interesting sessions** with `/share` to keep a record or get help.
7. **Ask for skills when relevant** — e.g. `/ponytail` for minimal changes,
   `improve` before big refactors.

## 8. Environment tips (Windows)

- Commands run in **PowerShell**; repo provides a `justfile` — use `just` recipes
  for everything you can (`start-env`, `backend-test`, `frontend-run`, ...).
- Docker must be running for backend tests (Testcontainers).
- Set `JWT_SECRET` in `.env` (via `just setup-env`) before starting the backend.

## 9. Useful docs

- Full config reference: <https://opencode.ai/docs/> and the JSON schema at
  <https://opencode.ai/config.json>.
- Project behavior: read `AGENTS.md` (root) and `.opencode/SYSTEM.md`.
