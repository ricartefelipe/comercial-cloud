#!/usr/bin/env bash
# Deploy ComercialCloud na EC2 via SSH (rsync + docker compose prod + Keycloak OIDC).
#
# Uso:
#   COMERCIAL_HOST=1.2.3.4 COMERCIAL_KEY=~/.ssh/comercial-cloud-deploy.pem ./scripts/aws-deploy-comercial-ec2.sh
#   source .aws-deploy/last-ec2.env && ./scripts/aws-deploy-comercial-ec2.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

COMERCIAL_HOST="${COMERCIAL_HOST:-}"
COMERCIAL_KEY="${COMERCIAL_KEY:-$HOME/.ssh/comercial-cloud-deploy.pem}"
COMERCIAL_USER="${COMERCIAL_USER:-ec2-user}"
REPO_URL="${REPO_URL:-git@github.com:ricartefelipe/comercial-cloud.git}"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/comercial-cloud/comercial-cloud}"

if [[ -z "$COMERCIAL_HOST" && -f "$ROOT/.aws-deploy/last-ec2.env" ]]; then
  # shellcheck disable=SC1091
  source "$ROOT/.aws-deploy/last-ec2.env"
  COMERCIAL_HOST="${PUBLIC_IP:-$COMERCIAL_HOST}"
  COMERCIAL_KEY="${KEY_FILE:-$COMERCIAL_KEY}"
  COMERCIAL_DOMAIN="${PILOT_DOMAIN:-$COMERCIAL_DOMAIN}"
fi

if [[ -z "$COMERCIAL_HOST" ]]; then
  echo "Defina COMERCIAL_HOST (IP público da EC2)." >&2
  exit 1
fi

COMERCIAL_DOMAIN="${COMERCIAL_DOMAIN:-${COMERCIAL_HOST//./-}.sslip.io}"
SCHEME="${SCHEME:-http}"
if [[ "$COMERCIAL_DOMAIN" == *sslip.io* ]]; then
  SCHEME="${SCHEME:-http}"
fi
BASE_URL="${SCHEME}://${COMERCIAL_DOMAIN}"
KEYCLOAK_PUBLIC="${BASE_URL}/auth"
KEYCLOAK_ISSUER="${KEYCLOAK_PUBLIC}/realms/comercialcloud"

SSH=(ssh -i "$COMERCIAL_KEY" -o StrictHostKeyChecking=accept-new "${COMERCIAL_USER}@${COMERCIAL_HOST}")

echo "▸ Aguardando SSH em $COMERCIAL_HOST..."
for _ in $(seq 1 30); do
  if "${SSH[@]}" "test -f /opt/comercial-cloud/bootstrap.ok || command -v docker >/dev/null" 2>/dev/null; then
    break
  fi
  sleep 10
done

"${SSH[@]}" "docker --version || sudo docker --version"
COMPOSE_CMD="$("${SSH[@]}" "command -v docker-compose >/dev/null 2>&1 && echo docker-compose || echo 'sudo docker compose'")"

ENV_FILE="${ENV_FILE:-$ROOT/.env.aws-pilot}"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "▸ Gerando $ENV_FILE (piloto — revisar secrets)"
  POSTGRES_PASSWORD="$(openssl rand -hex 16)"
  KEYCLOAK_ADMIN_PASSWORD="$(openssl rand -hex 12)"
  cat > "$ENV_FILE" <<EOF
DOMAIN=${COMERCIAL_DOMAIN}
APP_URL=${BASE_URL}
KEYCLOAK_PUBLIC_URL=${KEYCLOAK_PUBLIC}
KEYCLOAK_ISSUER_URL=${KEYCLOAK_ISSUER}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=${KEYCLOAK_ADMIN_PASSWORD}
COMERCIAL_CLOUD_KEYCLOAK_PROVISIONING_ENABLED=true
COMERCIAL_CLOUD_BILLING_ASAAS_ENABLED=false
COMERCIAL_CLOUD_FISCAL_FOCUSNFE_ENABLED=false
COMERCIAL_CLOUD_MAIL_ENABLED=false
MAIL_FROM=noreply@${COMERCIAL_DOMAIN}
EOF
  chmod 600 "$ENV_FILE"
fi

echo "▸ Gerando realm Keycloak para $BASE_URL"
"$ROOT/scripts/generate-keycloak-realm-prod.sh" "$BASE_URL"

echo "▸ Sincronizando código na EC2..."
"${SSH[@]}" "sudo mkdir -p /opt/comercial-cloud && sudo chown ${COMERCIAL_USER}:${COMERCIAL_USER} /opt/comercial-cloud"

RSYNC_SSH="ssh -i $COMERCIAL_KEY -o StrictHostKeyChecking=accept-new"
rsync -az --delete \
  -e "$RSYNC_SSH" \
  --exclude '.git' --exclude 'node_modules' --exclude 'target' \
  --exclude '.env' --exclude '.env.aws-pilot' --exclude '.aws-deploy' \
  --exclude 'frontend/.next' \
  "$ROOT/" "${COMERCIAL_USER}@${COMERCIAL_HOST}:${DEPLOY_DIR}/"

scp -i "$COMERCIAL_KEY" -o StrictHostKeyChecking=accept-new \
  "$ENV_FILE" "${COMERCIAL_USER}@${COMERCIAL_HOST}:${DEPLOY_DIR}/.env.aws-pilot"

# Atualizar URLs HTTPS se piloto já tem TLS
if [[ -f "$ROOT/.aws-deploy/last-ec2.env" ]]; then
  # shellcheck disable=SC1091
  source "$ROOT/.aws-deploy/last-ec2.env"
  if [[ -n "${PILOT_DOMAIN:-}" ]]; then
    HTTPS_BASE="https://${PILOT_DOMAIN}"
    "${SSH[@]}" "grep -q '^APP_URL=' ${DEPLOY_DIR}/.env.aws-pilot && sed -i 's|^APP_URL=.*|APP_URL=${HTTPS_BASE}|' ${DEPLOY_DIR}/.env.aws-pilot || true"
  fi
fi

echo "▸ Build + compose up (~15–25 min na t3.medium)..."
"${SSH[@]}" bash <<REMOTE
set -euo pipefail
cd ${DEPLOY_DIR}
sudo usermod -aG docker ${COMERCIAL_USER} 2>/dev/null || true
if ! swapon --show | grep -q /swapfile; then
  sudo fallocate -l 2G /swapfile 2>/dev/null || sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
fi
cp -f deploy/nginx/conf.d/default-pilot.conf deploy/nginx/conf.d/default-pilot-active.conf
COMPOSE="\$(command -v docker-compose || echo docker-compose)"
sudo \$COMPOSE --env-file .env.aws-pilot -f docker-compose.prod.yml build 2>&1 | tail -20
sudo \$COMPOSE --env-file .env.aws-pilot -f docker-compose.prod.yml up -d
sleep 45
sudo \$COMPOSE --env-file .env.aws-pilot -f docker-compose.prod.yml ps
curl -sf http://127.0.0.1/health && echo " nginx/backend OK" || echo " aguardando health..."
REMOTE

echo ""
echo "✔ Deploy ComercialCloud"
echo "  App:      ${BASE_URL}/"
echo "  API:      ${BASE_URL}/api/v1/public/planos"
echo "  Keycloak: ${KEYCLOAK_PUBLIC}/"
echo "  Login demo: admin@demo.com / admin123"
echo ""
echo "TLS (recomendado):"
echo "  source .aws-deploy/last-ec2.env"
echo "  CERTBOT_EMAIL=seu@email.com ./scripts/aws-setup-tls-ec2.sh"
