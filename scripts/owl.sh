#!/usr/bin/env bash
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="${HERE%/scripts}"

if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  DC=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  DC=(docker-compose)
else
  echo "Docker Compose is required (docker compose or docker-compose)." >&2
  exit 1
fi

usage() {
  cat << EOF
OWL dev helper

Usage: $(basename "$0") [start|stop|restart|status|logs|purge]

Commands:
  start      Start all services (builds app). Honors OWL_SECURITY_ENABLED and ISSUER_URI.
  stop       Stop services (preserve volumes)
  restart    Stop then start
  status     Show compose status
  logs       Tail app logs
  purge      Stop and remove volumes (DANGER: wipes data)

Env (optional):
  OWL_SECURITY_ENABLED=true|false (default: false)
  ISSUER_URI=http://keycloak:8080/realms/owl-dev

Examples:
  OWL_SECURITY_ENABLED=true ISSUER_URI=http://keycloak:8080/realms/owl-dev \
    $(basename "$0") start
EOF
}

start() {
  pushd "$ROOT" >/dev/null
  echo "Starting OWL stack..."
  # Export for compose to pick up
  export OWL_SECURITY_ENABLED="${OWL_SECURITY_ENABLED:-false}"
  export ISSUER_URI="${ISSUER_URI:-http://keycloak:8080/realms/owl-dev}"
  "${DC[@]}" up -d --build
  echo "Services up. App: http://localhost:8080  Keycloak: http://localhost:8081"
  echo "OpenAPI: http://localhost:8080/swagger-ui.html"
  popd >/dev/null
}

stop() {
  pushd "$ROOT" >/dev/null
  echo "Stopping OWL stack..."
  "${DC[@]}" down
  popd >/dev/null
}

purge() {
  pushd "$ROOT" >/dev/null
  read -r -p "This will REMOVE VOLUMES (Mongo/Qdrant/Keycloak). Continue? [y/N] " yn
  case "$yn" in
    [Yy]*) "${DC[@]}" down -v ;; 
    *) echo "Aborted." ;;
  esac
  popd >/dev/null
}

status() {
  pushd "$ROOT" >/dev/null
  "${DC[@]}" ps
  popd >/dev/null
}

logs() {
  pushd "$ROOT" >/dev/null
  "${DC[@]}" logs -f owl-app
  popd >/dev/null
}

case "${1:-start}" in
  start) start ;;
  stop) stop ;;
  restart) stop; start ;;
  status) status ;;
  logs) logs ;;
  purge) purge ;;
  -h|--help|help) usage ;;
  *) echo "Unknown command: ${1}" >&2; usage; exit 2 ;;
esac

