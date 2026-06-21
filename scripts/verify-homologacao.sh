#!/usr/bin/env bash
# Verifica serviços mínimos para homologação / piloto.
set -euo pipefail

API_URL="${API_URL:-http://localhost:8080}"
APP_URL="${APP_URL:-http://localhost:3000}"
AUTH_URL="${AUTH_URL:-http://localhost:8180}"

fail=0

check() {
  local name="$1"
  local url="$2"
  if curl -sf --max-time 10 "$url" >/dev/null; then
    echo "OK  $name — $url"
  else
    echo "FALHA $name — $url"
    fail=1
  fi
}

echo "=== ComercialCloud — verificação de homologação ==="
check "Backend health" "$API_URL/health"
check "Frontend" "$APP_URL"
check "OpenAPI" "$API_URL/q/openapi"

if curl -sf --max-time 5 "$AUTH_URL/realms/comercialcloud" >/dev/null 2>&1; then
  echo "OK  Keycloak realm — $AUTH_URL/realms/comercialcloud"
else
  echo "SKIP Keycloak (perfil opcional) — $AUTH_URL"
fi

if curl -sf --max-time 5 "http://localhost:8025" >/dev/null 2>&1; then
  echo "OK  MailHog UI — http://localhost:8025"
fi

if [[ "$fail" -eq 0 ]]; then
  echo ""
  echo "Homologação básica: VERIFICADA"
  exit 0
fi

echo ""
echo "Homologação básica: FALHOU — corrija os serviços acima"
exit 1
