#!/usr/bin/env bash
set -euo pipefail

MODE=${1:-}
CMD=${2:-}

OWL_NS=${OWL_NS:-owl}
GW_NS=${GW_NS:-gateway}

usage(){
  cat << EOF
Usage: $(basename "$0") <local|k8s> <up|down>

Examples:
  # Local (Docker Compose)
  bash scripts/deploy.sh local up
  bash scripts/deploy.sh local down

  # Kubernetes (Helm)
  bash scripts/deploy.sh k8s up
  bash scripts/deploy.sh k8s down

Env:
  OWL_NS   (default: owl)
  GW_NS    (default: gateway)
EOF
}

local_up(){
  make build-retrieval build-safety build-modelproxy || true
  docker compose up -d owl-app retrieval-service safety-service model-proxy
}
local_down(){
  docker compose down
}

k8s_up(){
  pushd deploy/helm/umbrella >/dev/null
  helm dependency build
  helm upgrade --install owl . -n "$OWL_NS" --create-namespace
  popd >/dev/null

  pushd deploy/helm/kong >/dev/null
  helm upgrade --install kong . -n "$GW_NS" --create-namespace
  popd >/dev/null
}

k8s_down(){
  helm uninstall owl -n "$OWL_NS" || true
  helm uninstall kong -n "$GW_NS" || true
}

case "$MODE:$CMD" in
  local:up) local_up ;;
  local:down) local_down ;;
  k8s:up) k8s_up ;;
  k8s:down) k8s_down ;;
  *) usage; exit 2 ;;
esac

