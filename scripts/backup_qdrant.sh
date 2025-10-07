#!/usr/bin/env bash
set -euo pipefail
QDRANT_URL=${QDRANT_URL:-http://localhost:6333}
OUT_DIR=${1:-backups/qdrant-$(date +%Y%m%d-%H%M%S)}
mkdir -p "$OUT_DIR"
echo "Triggering Qdrant snapshot (requires snapshots enabled)."
curl -sS -X POST "$QDRANT_URL/collections/${QDRANT_COLLECTION:-owl_kb}/snapshots" | tee "$OUT_DIR/response.json"
echo
echo "List snapshots: $QDRANT_URL/collections/${QDRANT_COLLECTION:-owl_kb}/snapshots"
