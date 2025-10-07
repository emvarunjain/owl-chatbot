#!/usr/bin/env bash
set -euo pipefail
OUT_DIR=${1:-backups/mongo-$(date +%Y%m%d-%H%M%S)}
mkdir -p "$OUT_DIR"
echo "Backing up Mongo to $OUT_DIR"
mongodump --uri "${MONGO_URI:-mongodb://localhost:27017/owl}" --out "$OUT_DIR"
echo "Done"
