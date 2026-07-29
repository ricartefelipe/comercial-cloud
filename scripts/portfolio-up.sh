#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "==> ComercialCloud: stack local (Compose)"
docker compose up -d --build

echo "==> Aguardando serviços"
for i in $(seq 1 90); do
  if curl -fsS "http://localhost:8080/q/health" >/dev/null 2>&1 \
    || curl -fsS "http://localhost:8080/actuator/health" >/dev/null 2>&1 \
    || curl -fsS "http://localhost:3000/" >/dev/null 2>&1; then
    echo "OK — demo local"
    echo "Frontend costuma ficar em :3000; API/Keycloak conforme docker-compose.yml"
    docker compose ps
    echo
    echo "Piloto EC2 (quando AWS ok): SCHEME=https ./scripts/aws-deploy-comercial-ec2.sh"
    exit 0
  fi
  sleep 5
done

echo "Timeout. Logs:" >&2
docker compose logs --tail=80
exit 1
