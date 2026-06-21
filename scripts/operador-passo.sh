#!/usr/bin/env bash
# Guia interativo das pendências do operador (ordem: Asaas → Focus → Keycloak → Mail → DNS → Backup → Jurídico).
#
# Uso:
#   ./scripts/operador-passo.sh                  # menu
#   ./scripts/operador-passo.sh asaas            # instruções Asaas + gerar webhook token
#   ./scripts/operador-passo.sh asaas-apply      # aplica .env.aws-pilot na EC2
#   ./scripts/operador-passo.sh focus-apply
#   ./scripts/operador-passo.sh keycloak-apply
#   ./scripts/operador-passo.sh mail-apply
#   ./scripts/operador-passo.sh dns
#   ./scripts/operador-passo.sh backup
#   ./scripts/operador-passo.sh juridico
#   ./scripts/operador-passo.sh verify
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT/.env.aws-pilot}"
EXAMPLE="$ROOT/.env.aws-pilot.example"
PILOT_DOMAIN="${PILOT_DOMAIN:-56-124-36-136.sslip.io}"
APP_URL="${APP_URL:-https://${PILOT_DOMAIN}}"

if [[ -f "$ROOT/.aws-deploy/last-ec2.env" ]]; then
  # shellcheck disable=SC1091
  source "$ROOT/.aws-deploy/last-ec2.env"
  PILOT_DOMAIN="${PILOT_DOMAIN:-56-124-36-136.sslip.io}"
  APP_URL="https://${PILOT_DOMAIN}"
fi

ensure_env() {
  if [[ ! -f "$ENV_FILE" ]]; then
    echo "▸ Criando $ENV_FILE a partir do example..."
    cp "$EXAMPLE" "$ENV_FILE"
    chmod 600 "$ENV_FILE"
    echo "   Edite o arquivo e rode novamente."
  fi
}

set_env_var() {
  local key="$1"
  local value="$2"
  if grep -q "^${key}=" "$ENV_FILE" 2>/dev/null; then
    sed -i "s|^${key}=.*|${key}=${value}|" "$ENV_FILE"
  else
    echo "${key}=${value}" >> "$ENV_FILE"
  fi
}

step_asaas() {
  ensure_env
  echo ""
  echo "═══ 1/7 Asaas ═══"
  echo "Webhook URL: ${APP_URL}/api/v1/public/webhooks/asaas"
  echo ""
  if ! grep -q '^ASAAS_WEBHOOK_TOKEN=.\+' "$ENV_FILE" 2>/dev/null; then
    token="$(openssl rand -hex 24)"
    set_env_var "ASAAS_WEBHOOK_TOKEN" "$token"
    echo "▸ ASAAS_WEBHOOK_TOKEN gerado e salvo em $ENV_FILE"
  else
    echo "▸ ASAAS_WEBHOOK_TOKEN já definido em $ENV_FILE"
  fi
  echo ""
  echo "No painel Asaas:"
  echo "  1. Integrações → API → copiar API Key"
  echo "  2. Configurar webhook com a URL acima"
  echo "  3. Colar no $ENV_FILE:"
  echo "     COMERCIAL_CLOUD_BILLING_GATEWAY=asaas"
  echo "     COMERCIAL_CLOUD_BILLING_ASAAS_ENABLED=true"
  echo "     ASAAS_API_KEY=<sua_chave>"
  echo ""
  echo "Depois: ./scripts/operador-passo.sh asaas-apply"
  echo "Guia completo: docs/OPERADOR-PASSO-A-PASSO.md#1-asaas-billing--pix-pdv"
}

step_asaas_apply() {
  ensure_env
  if ! grep -q '^ASAAS_API_KEY=.\+' "$ENV_FILE"; then
    echo "AVISO: ASAAS_API_KEY ainda vazio em $ENV_FILE — configure antes de ir a produção." >&2
  fi
  set_env_var "COMERCIAL_CLOUD_BILLING_GATEWAY" "asaas"
  set_env_var "COMERCIAL_CLOUD_BILLING_ASAAS_ENABLED" "true"
  "$ROOT/scripts/operador-aplicar-env-ec2.sh"
}

step_focus() {
  ensure_env
  echo ""
  echo "═══ 2/7 Focus NFe ═══"
  echo "Configure em $ENV_FILE:"
  echo "  COMERCIAL_CLOUD_FISCAL_PROVIDER=focusnfe"
  echo "  COMERCIAL_CLOUD_FISCAL_FOCUSNFE_ENABLED=true"
  echo "  FOCUSNFE_TOKEN=<token>"
  echo ""
  echo "Retaguarda → Fiscal → ambiente HOMOLOGACAO primeiro."
  echo "Depois: ./scripts/operador-passo.sh focus-apply"
}

step_focus_apply() {
  ensure_env
  set_env_var "COMERCIAL_CLOUD_FISCAL_PROVIDER" "focusnfe"
  set_env_var "COMERCIAL_CLOUD_FISCAL_FOCUSNFE_ENABLED" "true"
  "$ROOT/scripts/operador-aplicar-env-ec2.sh"
}

step_keycloak() {
  echo ""
  echo "═══ 3/7 Keycloak ═══"
  "$ROOT/scripts/generate-keycloak-realm-prod.sh" "$APP_URL"
  echo ""
  echo "Importe infra/keycloak/realm-prod.json no admin Keycloak se necessário."
  echo "Depois: ./scripts/operador-passo.sh keycloak-apply"
}

step_keycloak_apply() {
  ensure_env
  set_env_var "COMERCIAL_CLOUD_KEYCLOAK_PROVISIONING_ENABLED" "true"
  set_env_var "KEYCLOAK_PUBLIC_URL" "${APP_URL}/auth"
  set_env_var "KEYCLOAK_ISSUER_URL" "${APP_URL}/auth/realms/comercialcloud"
  "$ROOT/scripts/operador-aplicar-env-ec2.sh"
}

step_mail() {
  ensure_env
  echo ""
  echo "═══ 4/7 SMTP ═══"
  echo "Configure MAIL_HOST, MAIL_USER, MAIL_PASSWORD, MAIL_FROM em $ENV_FILE"
  echo "  COMERCIAL_CLOUD_MAIL_ENABLED=true"
  echo "  COMERCIAL_CLOUD_MAIL_APP_URL=${APP_URL}"
  echo "Depois: ./scripts/operador-passo.sh mail-apply"
}

step_mail_apply() {
  ensure_env
  set_env_var "COMERCIAL_CLOUD_MAIL_ENABLED" "true"
  set_env_var "COMERCIAL_CLOUD_MAIL_APP_URL" "$APP_URL"
  "$ROOT/scripts/operador-aplicar-env-ec2.sh"
}

step_dns() {
  echo ""
  echo "═══ 5/7 DNS ═══"
  COMERCIAL_HOST="${PUBLIC_IP:-}" "$ROOT/scripts/aws-setup-dns-auto.sh" || true
  echo ""
  echo "Domínio próprio: atualize DOMAIN/APP_URL no .env.aws-pilot e rode aws-setup-tls-ec2.sh + deploy."
}

step_backup() {
  echo ""
  echo "═══ 6/7 Backup ═══"
  "$ROOT/scripts/install-backup-cron-ec2.sh"
}

step_juridico() {
  echo ""
  echo "═══ 7/7 Jurídico ═══"
  echo "Revise: docs/CHECKLIST-JURIDICO.md"
  echo "Páginas: ${APP_URL}/termos e ${APP_URL}/privacidade"
  echo "Contrato piloto: docs/PILOTO-WHITELABEL.md"
}

step_verify() {
  APP_URL="$APP_URL" API_URL="$APP_URL" "$ROOT/scripts/verify-operador-piloto.sh"
}

show_menu() {
  cat <<EOF

ComercialCloud — operador (ordem recomendada)
  1) asaas          — gerar token webhook + instruções
  2) asaas-apply    — aplicar Asaas na EC2
  3) focus          — instruções Focus NFe
  4) focus-apply    — aplicar Focus na EC2
  5) keycloak       — gerar realm
  6) keycloak-apply — aplicar Keycloak na EC2
  7) mail           — instruções SMTP
  8) mail-apply     — aplicar mail na EC2
  9) dns            — verificar sslip.io
 10) backup         — cron PostgreSQL na EC2
 11) juridico       — checklist
 12) verify         — testes do piloto

Documentação: docs/OPERADOR-PASSO-A-PASSO.md

EOF
}

cmd="${1:-menu}"
case "$cmd" in
  menu|help|-h|--help) show_menu ;;
  asaas) step_asaas ;;
  asaas-apply) step_asaas_apply ;;
  focus) step_focus ;;
  focus-apply) step_focus_apply ;;
  keycloak) step_keycloak ;;
  keycloak-apply) step_keycloak_apply ;;
  mail) step_mail ;;
  mail-apply) step_mail_apply ;;
  dns) step_dns ;;
  backup) step_backup ;;
  juridico) step_juridico ;;
  verify) step_verify ;;
  *)
    echo "Comando desconhecido: $cmd" >&2
    show_menu
    exit 1
    ;;
esac
