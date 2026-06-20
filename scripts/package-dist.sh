#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIST_DIR="${1:-$ROOT/../dist}"
OUT="$DIST_DIR/comercial-cloud"

rm -rf "$OUT"
mkdir -p "$OUT"

rsync -a \
  --exclude '.git' \
  --exclude 'node_modules' \
  --exclude 'frontend/.next' \
  --exclude 'backend/target' \
  --exclude 'e2e/node_modules' \
  --exclude 'e2e/test-results' \
  --exclude 'e2e/playwright-report' \
  "$ROOT/" "$OUT/"

(
  cd "$DIST_DIR"
  rm -f comercial-cloud.zip
  zip -rq comercial-cloud.zip comercial-cloud
)

echo "Pacote gerado: $DIST_DIR/comercial-cloud.zip"
