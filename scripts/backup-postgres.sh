#!/usr/bin/env bash
# Backup PostgreSQL do ComercialCloud.
# Uso: ./scripts/backup-postgres.sh [diretorio_destino]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="${1:-$ROOT/../backups}"
TS="$(date +%Y%m%d-%H%M%S)"
FILE="$DEST/comercialcloud-$TS.sql.gz"

DB_NAME="${POSTGRES_DB:-comercialcloud}"
DB_USER="${POSTGRES_USER:-comercialcloud}"
DB_HOST="${POSTGRES_HOST:-localhost}"
DB_PORT="${POSTGRES_PORT:-5432}"

mkdir -p "$DEST"

if docker ps --format '{{.Names}}' | grep -qx comercialcloud-postgres; then
  docker exec comercialcloud-postgres \
    pg_dump -U "$DB_USER" -d "$DB_NAME" --no-owner --clean | gzip > "$FILE"
else
  PGPASSWORD="${POSTGRES_PASSWORD:-comercialcloud}" \
    pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" --no-owner --clean | gzip > "$FILE"
fi

echo "Backup salvo: $FILE"
find "$DEST" -name 'comercialcloud-*.sql.gz' -mtime +14 -delete 2>/dev/null || true
