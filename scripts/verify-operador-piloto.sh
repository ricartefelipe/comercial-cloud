#!/usr/bin/env bash
# Verifica piloto após configuração do operador.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [[ -f "$ROOT/.aws-deploy/last-ec2.env" ]]; then
  # shellcheck disable=SC1091
  source "$ROOT/.aws-deploy/last-ec2.env"
fi

APP_URL="${APP_URL:-https://${PILOT_DOMAIN:-56-125-12-61.sslip.io}}"
API_URL="${API_URL:-$APP_URL}"
AUTH_URL="${AUTH_URL:-${APP_URL}/auth}"

fail=0
warn=0

ok() { echo "OK   $1"; }
fail_msg() { echo "FALHA $1"; fail=1; }
warn_msg() { echo "AVISO $1"; warn=1; }

echo "=== Verificação operador — $APP_URL ==="

if curl -sf --max-time 15 "${API_URL}/health" >/dev/null; then
  ok "Backend health"
else
  fail_msg "Backend health — ${API_URL}/health"
fi

if curl -sf --max-time 15 "${APP_URL}/login" >/dev/null; then
  ok "Frontend login"
else
  fail_msg "Frontend — ${APP_URL}/login"
fi

if curl -sf --max-time 15 "${AUTH_URL}/realms/comercialcloud" >/dev/null; then
  ok "Keycloak realm"
else
  warn_msg "Keycloak — ${AUTH_URL}/realms/comercialcloud"
fi

# Webhook: POST vazio deve responder (401 se token configurado, 200 se evento ignorado)
webhook_code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 \
  -X POST "${API_URL}/api/v1/public/webhooks/asaas" \
  -H 'Content-Type: application/json' \
  -d '{}' || echo "000")"
if [[ "$webhook_code" =~ ^(200|401)$ ]]; then
  ok "Webhook Asaas acessível (HTTP $webhook_code)"
else
  fail_msg "Webhook Asaas — HTTP $webhook_code (esperado 200 ou 401)"
fi

if curl -sf --max-time 15 "${API_URL}/api/v1/public/planos" >/dev/null; then
  ok "API pública planos"
else
  warn_msg "Planos públicos"
fi

echo ""
if [[ "$fail" -eq 0 ]]; then
  echo "Verificação: OK (${warn} aviso(s))"
  exit 0
fi
echo "Verificação: FALHOU"
exit 1
