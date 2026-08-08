#!/usr/bin/env bash
#
# get-started.sh — one-shot bootstrap for a new Darb developer.
#
# Installs and configures everything the project needs:
#   git, docker (+ compose), just, Node.js (>=22), Java (Temurin 25), openssl
# Then prepares local env files (creates .env / frontend/.env.local and
# generates a JWT_SECRET when missing) and prints the next steps.
#
# Works on: Linux (apt/dnf/pacman/apk), macOS (Homebrew),
#           Windows (Git Bash + winget, falls back to scoop/choco).
#
# Usage:
#   ./get-started.sh
#
set -euo pipefail

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; CYAN='\033[0;36m'; NC='\033[0m'

info()  { printf "${CYAN}[i]${NC} %s\n" "$*"; }
ok()    { printf "${GREEN}[ok]${NC} %s\n" "$*"; }
warn()  { printf "${YELLOW}[!]${NC} %s\n" "$*"; }
fail()  { printf "${RED}[x]${NC} %s\n" "$*"; }

has() { command -v "$1" >/dev/null 2>&1; }

SUDO=()
if [[ "$(id -u)" -ne 0 ]] && has sudo; then
  SUDO=(sudo)
fi

# ── OS detection ──────────────────────────────────────────────
OS=unknown
case "$(uname -s)" in
  Linux)  OS=linux ;;
  Darwin) OS=macos ;;
  MINGW*|MSYS*|CYGWIN*) OS=windows ;;
  *) warn "Unsupported platform: $(uname -s)";;
esac

PKG_MGR=none
if [[ "$OS" == "linux" ]]; then
  if has apt-get; then PKG_MGR=apt
  elif has dnf; then PKG_MGR=dnf
  elif has pacman; then PKG_MGR=pacman
  elif has apk; then PKG_MGR=apk
  fi
fi

# ── helpers ───────────────────────────────────────────────────
ensure() { # ensure <name> <message>
  if has "$1"; then ok "$1 already installed"; else warn "$1 — $2"; fi
}

install_just_binary() { # cross-platform just from GitHub releases
  local ver=1.40.0 target os arch
  case "$(uname -s)" in
    Linux) os="unknown-linux-musl";;
    Darwin) os="apple-darwin";;
    *) warn "  just: manual install needed for this platform"; return 1;;
  esac
  case "$(uname -m)" in
    x86_64|amd64) arch="x86_64";;
    aarch64|arm64) arch="aarch64";;
    *) warn "  just: unsupported arch $(uname -m)"; return 1;;
  esac
  target="${arch}-${os}"
  local url="https://github.com/casey/just/releases/download/${ver}/just-${ver}-${target}.tar.gz"
  info "Downloading just ${ver} (${target})..."
  curl -fsSL "$url" -o /tmp/just.tar.gz
  tar -xzf /tmp/just.tar.gz -C /tmp just
  mkdir -p "$HOME/.local/bin"
  mv -f /tmp/just "$HOME/.local/bin/just"
  if [[ ":$PATH:" != *":$HOME/.local/bin:"* ]]; then
    export PATH="$HOME/.local/bin:$PATH"
    add_to_shell_profile 'export PATH="$HOME/.local/bin:$PATH"'
  fi
  ok "just ${ver} installed at ~/.local/bin/just"
}

add_to_shell_profile() { # idempotent line injection into ~/.bashrc / ~/.zshrc
  local line="$1" rc
  for rc in "$HOME/.bashrc" "$HOME/.zshrc"; do
    [[ -f "$rc" ]] || continue
    grep -qF -- "$line" "$rc" || printf '\n%s\n' "$line" >> "$rc"
  done
}

install_temurin() { # Temurin JDK 25 tarball into ~/.local/opt
  local os arch ver=25
  case "$(uname -s)" in
    Linux) os="linux";;
    Darwin) os="mac";;
    *) warn "  java: manual install needed"; return 1;;
  esac
  case "$(uname -m)" in
    x86_64|amd64) arch="x64";;
    aarch64|arm64) arch="aarch64";;
    *) warn "  java: unsupported arch"; return 1;;
  esac
  local url="https://api.adoptium.net/v3/binary/latest/${ver}/ga/${os}/${arch}/jdk/hotspot/normal/eclipse"
  info "Downloading Temurin JDK ${ver} (${os}-${arch})..."
  curl -fsSL "$url" -o /tmp/temurin.tar.gz
  mkdir -p "$HOME/.local/opt"
  tar -xzf /tmp/temurin.tar.gz -C "$HOME/.local/opt"
  local jdk_dir
  jdk_dir=$(find "$HOME/.local/opt" -maxdepth 1 -type d -name 'jdk-*' | head -n1)
  add_to_shell_profile "export JAVA_HOME=\"$jdk_dir\""
  add_to_shell_profile 'export PATH="$JAVA_HOME/bin:$PATH"'
  export JAVA_HOME="$jdk_dir"
  export PATH="$JAVA_HOME/bin:$PATH"
  ok "Temurin JDK ${ver} installed at $jdk_dir"
}

check_java() {
  if has java; then
    local major
    major=$(java -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')
    if [[ "${major:-0}" == "25" ]]; then ok "java 25 (${major})"
    else warn "java is version ${major:-unknown}; project requires 25 — installing Temurin 25"; install_temurin; fi
  else
    info "java not found — installing Temurin 25"; install_temurin
  fi
}

check_node() {
  if has node; then
    local major
    major=$(node -v | sed 's/^v\([0-9]*\).*/\1/')
    if (( major >= 22 )); then ok "node $(node -v) (npm $(npm -v 2>/dev/null || echo '?'))"
    else warn "node is v${major}; project needs >=22 — installing LTS"; install_node; fi
  else
    info "node not found — installing LTS"; install_node
  fi
}

check_docker() {
  if has docker && docker compose version >/dev/null 2>&1; then
    ok "docker + compose"
  else
    warn "docker (with compose) missing/not usable"
    install_docker || warn "  start Docker manually (e.g. 'Docker Desktop') then re-run."
  fi
}

# ── installers per platform ───────────────────────────────────
install_linux_pkgs() {
  case "$PKG_MGR" in
    apt)
      info "apt: updating package lists..."
      "${SUDO[@]}" apt-get update -y
      "${SUDO[@]}" apt-get install -y curl ca-certificates gnupg git openssl tar >/dev/null
      if ! has docker; then
        info "Installing Docker via official script..."
        curl -fsSL https://get.docker.com | "${SUDO[@]}" sh
        "${SUDO[@]}" usermod -aG docker "$USER" || true
        warn "  log out/in (or run 'newgrp docker') for docker group to apply"
      fi
      if ! has node; then
        info "Installing Node.js 22 via NodeSource..."
        curl -fsSL https://deb.nodesource.com/setup_22.x | "${SUDO[@]}" -E bash - >/dev/null || warn "  NodeSource failed; install Node.js 22 manually"
        "${SUDO[@]}" apt-get install -y nodejs >/dev/null || true
      fi
      ;;
    dnf)
      "${SUDO[@]}" dnf install -y git openssl curl >/dev/null
      if ! has docker; then "${SUDO[@]}" dnf install -y docker-ce docker-ce-cli docker-compose-plugin 2>/dev/null \
        || "${SUDO[@]}" dnf install -y docker docker-compose-plugin 2>/dev/null \
        || warn "  could not install docker via dnf — see docs.docker.com"; fi
      if ! has node; then "${SUDO[@]}" dnf module install -y nodejs:22 2>/dev/null || "${SUDO[@]}" dnf install -y nodejs || true; fi
      ;;
    pacman)
      "${SUDO[@]}" pacman -Sy --noconfirm git openssl curl >/dev/null
      if ! has docker; then "${SUDO[@]}" pacman -S --noconfirm docker docker-compose docker-buildx || true; fi
      if ! has node; then "${SUDO[@]}" pacman -S --noconfirm nodejs npm || true; fi
      ;;
    apk)
      "${SUDO[@]}" apk add --no-cache git openssl curl tar >/dev/null
      if ! has docker; then warn "  docker not available on alpine by default — install via the OS/distro"; fi
      if ! has node; then "${SUDO[@]}" apk add --no-cache nodejs npm >/dev/null || true; fi
      ;;
    *) warn "  no supported package manager — install git/docker/node manually";;
  esac
  [[ -n "${WSL_DISTRO_NAME:-}" ]] && warn "  you are on WSL — use 'docker desktop' with WSL2 backend"
}

install_macos() {
  if ! has brew; then
    warn "Homebrew missing — installing..."
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    eval "$(/opt/homebrew/bin/brew shellenv 2>/dev/null || /usr/local/bin/brew shellenv)"
  fi
  has git || brew install git >/dev/null
  has node || brew install node >/dev/null
  if ! has docker; then
    info "Installing Docker Desktop (cask)..."
    brew install --cask docker || warn "  start Docker Desktop once installed"
  fi
  has just || brew install just >/dev/null
}

winget_already_installed() {
  case "$1" in
    "Git.Git") has git ;;
    "Casey.Just") has just ;;
    "OpenJS.NodeJS.LTS") has node ;;
    "EclipseAdoptium.Temurin.25.JDK") has java ;;
    "Docker.DockerDesktop") has docker ;;
  esac
}

install_windows() {
  if has winget; then
    info "Installing tools via winget..."
    local ids=( "Git.Git" "Casey.Just" "OpenJS.NodeJS.LTS" "EclipseAdoptium.Temurin.25.JDK" "Docker.DockerDesktop" )
    for id in "${ids[@]}"; do
      if winget_already_installed "$id"; then ok "${id%%\.*} already present"; continue; fi
      info "winget install $id ..."
      winget install --id "$id" --silent --accept-source-agreements --accept-package-agreements --disable-interactivity \
        || warn "  winget install $id failed — install it manually"
    done
  elif has scoop; then
    scoop install git just openjdk25 nodejs-lts docker 2>/dev/null || true
  elif has choco; then
    choco install -y git just openjdk25 nodejs-lts docker-desktop 2>/dev/null || true
  else
    warn "  no winget/scoop/choco found — install git, Docker Desktop, Node.js 22, Temurin 25, just manually"
  fi
  warn "  Docker Desktop: open it once and accept the license before running 'just start-env'."
  warn "  Restart Git Bash / your terminal so newly installed tools are on PATH."
}

install_docker() { :; } # dispatch handled inside per-platform installers

# ── project env files ─────────────────────────────────────────
setup_env_files() {
  info "Preparing env files..."
  if [[ ! -f .env ]]; then cp .env.example .env; ok ".env created from .env.example"; else ok ".env exists"; fi

  if ! grep -qE '^JWT_SECRET=.+' .env; then
    local secret
    if has openssl; then secret=$(openssl rand -base64 32 | tr -d '\n')
    else secret=$(LC_ALL=C tr -dc 'A-Za-z0-9+/' </dev/urandom | head -c 48); fi
    sed -i.bak "s|^JWT_SECRET=.*|JWT_SECRET=${secret}|" .env
    rm -f .env.bak
    ok "JWT_SECRET generated"
  else
    ok "JWT_SECRET already set"
  fi

  if [[ ! -f frontend/.env.local ]]; then
    cp frontend/.env.example frontend/.env.local
    ok "frontend/.env.local created from example"
  else
    ok "frontend/.env.local exists"
  fi
}

# ── main ──────────────────────────────────────────────────────
main() {
  echo
  info "Darb — environment setup"
  info "OS: $OS / pkg: $PKG_MGR"

  case "$OS" in
    linux)   install_linux_pkgs ;;
    macos)   install_macos ;;
    windows) install_windows ;;
  esac

  has just || install_just_binary

  check_java
  check_node
  check_docker
  ensure git "git should be installed by now (see above)"
  has openssl && ok "openssl" || warn "openssl missing (needed for JWT generation)"

  setup_env_files

  echo
  ok "Done! Next steps:"
  printf "  ${GREEN}1.${NC} make sure Docker is running:   docker info\n"
  printf "  ${GREEN}2.${NC} start everything:               just start-env\n"
  printf "  ${GREEN}3.${NC} or step by step:                just setup-env && just infra-up && just backend-build\n"
  printf "  ${GREEN}4.${NC} list all commands:              just --list\n"
  echo
}

main "$@"
