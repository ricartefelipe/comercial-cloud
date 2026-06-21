#!/usr/bin/env bash
# Emite certificado Let's Encrypt na EC2 piloto e ativa HTTPS no nginx.
#
# Uso:
#   source .aws-deploy/last-ec2.env
#   ./scripts/aws-setup-dns-auto.sh && CERTBOT_EMAIL=seu@email.com ./scripts/aws-setup-tls-ec2.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

COMERCIAL_DOMAIN="${COMERCIAL_DOMAIN:-}"
COMERCIAL_HOST="${COMERCIAL_HOST:-}"
COMERCIAL_KEY="${COMERCIAL_KEY:-$HOME/.ssh/comercial-cloud-deploy.pem}"
COMERCIAL_USER="${COMERCIAL_USER:-ec2-user}"
CERTBOT_EMAIL="${CERTBOT_EMAIL:-}"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/comercial-cloud/comercial-cloud}"
ENV_FILE="${ENV_FILE:-$ROOT/.env.aws-pilot}"

if [[ -z "$COMERCIAL_HOST" && -f "$ROOT/.aws-deploy/last-ec2.env" ]]; then
  # shellcheck disable=SC1091
  source "$ROOT/.aws-deploy/last-ec2.env"
  COMERCIAL_HOST="${PUBLIC_IP:-}"
  COMERCIAL_KEY="${KEY_FILE:-$COMERCIAL_KEY}"
  COMERCIAL_DOMAIN="${COMERCIAL_DOMAIN:-${PILOT_DOMAIN:-}}"
fi

if [[ -z "$COMERCIAL_DOMAIN" && -n "$COMERCIAL_HOST" ]]; then
  COMERCIAL_DOMAIN="${COMERCIAL_HOST//./-}.sslip.io"
fi

if [[ -z "$COMERCIAL_HOST" || -z "$COMERCIAL_DOMAIN" ]]; then
  echo "Defina COMERCIAL_HOST / .aws-deploy/last-ec2.env" >&2
  exit 1
fi

SSH=(ssh -i "$COMERCIAL_KEY" -o StrictHostKeyChecking=accept-new "${COMERCIAL_USER}@${COMERCIAL_HOST}")

echo "▸ Verificando DNS: $COMERCIAL_DOMAIN → $COMERCIAL_HOST"
RESOLVED="$(dig +short "$COMERCIAL_DOMAIN" A 2>/dev/null | head -1 || true)"
if [[ "$RESOLVED" != "$COMERCIAL_HOST" ]]; then
  echo "DNS não aponta para a EC2. Rode aws-setup-dns-auto.sh" >&2
  exit 1
fi

RSYNC_SSH="ssh -i $COMERCIAL_KEY -o StrictHostKeyChecking=accept-new"
rsync -az -e "$RSYNC_SSH" \
  "$ROOT/deploy/nginx/conf.d/default-pilot-ssl.conf" \
  "$ROOT/docker-compose.prod.tls.yml" \
  "${COMERCIAL_USER}@${COMERCIAL_HOST}:${DEPLOY_DIR}/"

"${SSH[@]}" "mkdir -p ${DEPLOY_DIR}/scripts"
rsync -az -e "$RSYNC_SSH" \
  "$ROOT/scripts/aws-setup-tls-ec2-remote.sh" \
  "${COMERCIAL_USER}@${COMERCIAL_HOST}:${DEPLOY_DIR}/scripts/"

rsync -az -e "$RSYNC_SSH" \
  "$ROOT/deploy/nginx/conf.d/" \
  "${COMERCIAL_USER}@${COMERCIAL_HOST}:${DEPLOY_DIR}/deploy/nginx/conf.d/"

HTTPS_BASE="https://${COMERCIAL_DOMAIN}"
if [[ -f "$ENV_FILE" ]]; then
  sed -i "s|^DOMAIN=.*|DOMAIN=${COMERCIAL_DOMAIN}|" "$ENV_FILE"
  sed -i "s|^APP_URL=.*|APP_URL=${HTTPS_BASE}|" "$ENV_FILE"
  sed -i "s|^KEYCLOAK_PUBLIC_URL=.*|KEYCLOAK_PUBLIC_URL=${HTTPS_BASE}/auth|" "$ENV_FILE"
  sed -i "s|^KEYCLOAK_ISSUER_URL=.*|KEYCLOAK_ISSUER_URL=${HTTPS_BASE}/auth/realms/comercialcloud|" "$ENV_FILE"
  "$ROOT/scripts/generate-keycloak-realm-prod.sh" "$HTTPS_BASE"
  scp -i "$COMERCIAL_KEY" -o StrictHostKeyChecking=accept-new \
    "$ENV_FILE" "${COMERCIAL_USER}@${COMERCIAL_HOST}:${DEPLOY_DIR}/.env.aws-pilot"
  scp -i "$COMERCIAL_KEY" -o StrictHostKeyChecking=accept-new \
    "$ROOT/infra/keycloak/realm-prod.json" \
    "${COMERCIAL_USER}@${COMERCIAL_HOST}:${DEPLOY_DIR}/infra/keycloak/realm-prod.json"
fi

"${SSH[@]}" bash <<REMOTE
set -euo pipefail
cd ${DEPLOY_DIR}
export COMERCIAL_DOMAIN=${COMERCIAL_DOMAIN}
export CERTBOT_EMAIL=${CERTBOT_EMAIL}
bash scripts/aws-setup-tls-ec2-remote.sh
REMOTE

echo "✔ TLS ativo em ${HTTPS_BASE}"
