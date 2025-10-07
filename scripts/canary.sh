#!/usr/bin/env bash
set -euo pipefail

check() {
  local name=$1 url=$2
  echo "Checking $name at $url ..."
  if ! curl -fsS "$url" >/dev/null; then
    echo "[FAIL] $name not healthy: $url" >&2
    exit 1
  fi
}

check "retrieval-service" "http://localhost:9090/actuator/health"
check "safety-service" "http://localhost:9091/actuator/health"
check "model-proxy" "http://localhost:9092/actuator/health"
echo "All canaries healthy."
