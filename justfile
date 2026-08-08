# Darb development recipes.
# Requires: just (https://just.systems), Docker, Java 25, Node 22, npm.
# Install just on Windows:  scoop install just   (or winget install just)

set shell := ["powershell.exe", "-NoLogo", "-Command"]

# List available recipes
default:
    @just --list

# Start everything: prepare env files, restart infra, build backend & frontend, launch dev servers
start-env: setup-env infra-restart backend-build frontend-install start-servers
    @echo ""
    @echo "== Darb is starting =="
    @echo "  Backend : http://localhost:8080  (new window: backend dev server)"
    @echo "  Frontend: http://localhost:3000  (new window: frontend dev server)"
    @echo "  Stop the infra with: just stop"

# Create .env / frontend/.env.local from examples and generate JWT_SECRET if missing
setup-env:
    @if (Test-Path .env) { echo "  .env already exists" } else { Copy-Item .env.example .env; echo "  .env created from .env.example" }
    @if (Test-Path frontend/.env.local) { echo "  frontend/.env.local already exists" } else { Copy-Item frontend/.env.example frontend/.env.local; echo "  frontend/.env.local created" }
    @if (Select-String -Path .env -Pattern '^JWT_SECRET=.+' -Quiet) { echo "  JWT_SECRET already set" } else { $s = [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Max 256 })); (Get-Content .env) -replace '^JWT_SECRET=.*', "JWT_SECRET=$s" | Set-Content .env; echo "  JWT_SECRET generated in .env" }

# Bring up docker compose services (postgres + backend image)
infra-up:
    docker compose up -d

# Bring down docker compose services (keeps the pgdata volume)
infra-down:
    docker compose down

# Restart infra from scratch
infra-restart: infra-down
    docker compose up -d --build

# Build the backend (Maven clean install, tests skipped)
backend-build:
    @& ./backend/mvnw.cmd -f backend/pom.xml clean install -DskipTests

# Run the backend test suite
backend-test:
    @& ./backend/mvnw.cmd -f backend/pom.xml test

# Run the backend dev server (applies Flyway migrations on boot)
backend-run:
    @& ./backend/mvnw.cmd -f backend/pom.xml spring-boot:run

# Install frontend dependencies
frontend-install:
    npm --prefix frontend ci

# Run the frontend dev server
frontend-run:
    npm --prefix frontend run dev

# Launch backend + frontend dev servers in their own windows
start-servers:
    @Start-Process powershell -ArgumentList @('-NoExit', '-Command', '& ./backend/mvnw.cmd -f backend/pom.xml spring-boot:run')
    @Start-Process powershell -ArgumentList @('-NoExit', '-Command', 'npm --prefix frontend run dev')

# Stop the docker compose services
stop: infra-down
    @echo "  Infra stopped. Dev server windows are still open."

# Stop everything and wipe the database volume
reset:
    docker compose down -v
    @echo "  Volumes removed. Run 'just start-env' for a clean start."
