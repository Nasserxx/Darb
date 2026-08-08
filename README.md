# Darb
## the idea of the project

## Frontend

The SPA lives under `frontend/` (React, Vite, TypeScript). Auth flows (register, login, JWT session, change password) use locale-prefixed routes and i18n (`en`, `ar`, `de`).

- **Setup and dev server:** [frontend/README.md](frontend/README.md) — `npm install`, `npm run dev` (http://localhost:3000), `VITE_API_URL`
- **Architecture:** [frontend/docs/IMPLEMENTATION.md](frontend/docs/IMPLEMENTATION.md)
- **Adding features/routes:** [frontend/docs/EXTENDING.md](frontend/docs/EXTENDING.md)

Run the backend on port 8089 and set `CORS_ALLOWED_ORIGINS` to include the frontend origin (see repo `.env.example`).

## Backend toolchain

The backend lives under `backend/`. **Do not change Java or Spring Boot versions ad hoc**; align with the table below and keep them in one place (the parent POM and `java.version`).

| Piece | Pinned value | Where it is defined |
| --- | --- | --- |
| Java | 25 (major only) | `backend/pom.xml` → `java.version`, `backend/.java-version` |
| Spring Boot | 4.0.5 | `backend/pom.xml` → `spring-boot-starter-parent` |
| Apache Maven (wrapper) | 3.9.14 | `backend/.mvn/wrapper/maven-wrapper.properties` |
| Container runtime (Jib-style build) | Eclipse Temurin 25 (JDK in build, JRE in run) | `backend/Dockerfile` `FROM` lines |

- Build with **`./mvnw`** from the `backend/` directory so everyone uses the same Maven version.
- Library versions for Spring projects (for example Lombok, PostgreSQL driver) come from the **Spring Boot 4.0.5 dependency BOM**; see the [Spring Boot dependency versions](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html) appendix. To list what Maven resolves on your machine: `cd backend && ./mvnw -q dependency:list`.
- The build runs the **Maven Enforcer** plugin: if your JDK is not Java 25, or your Maven is older than 3.9.0, the build will fail with a message pointing back here.
- Set `JWT_SECRET` before first run (see `.env.example`). The `dev` profile will not start without it.

## Verification

CI runs on every PR (`.github/workflows/ci.yml`):

- **Backend:** `cd backend && ./mvnw test` (requires Docker for Testcontainers)
- **Frontend:** `cd frontend && npm ci && npm run lint && npm run build`
