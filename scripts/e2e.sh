#!/usr/bin/env bash
# Startet einen isolierten Stack (eigener Projektname, eigenes Volume, Zufallspasswort),
# führt Playwright (E2E + axe) aus und räumt danach auf – auch bei Fehlern.
set -euo pipefail
cd "$(dirname "$0")/.."

export COMPOSE_PROJECT_NAME="ultimatetodo-e2e"
export POSTGRES_PASSWORD="$(openssl rand -hex 16)"
export WEB_PORT="${E2E_WEB_PORT:-18080}"
export E2E_BASE_URL="http://localhost:${WEB_PORT}"
# Die Tests legen viele Konten von einer Adresse an; nur für diesen isolierten Lauf werden die Grenzen angehoben.
export RATE_LIMIT_LOGIN_ATTEMPTS=1000
export RATE_LIMIT_REGISTER_ATTEMPTS=1000

cleanup() { docker compose -p "$COMPOSE_PROJECT_NAME" -f compose.yaml down --volumes --remove-orphans >/dev/null 2>&1 || true; }
trap cleanup EXIT

docker compose -p "$COMPOSE_PROJECT_NAME" -f compose.yaml up -d --build --wait
cd src/frontend
npx playwright test
