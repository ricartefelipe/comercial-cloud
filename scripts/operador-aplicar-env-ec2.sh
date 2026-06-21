#!/usr/bin/env bash
# Aplica variáveis do .env.aws-pilot na EC2 e reinicia backend (+ frontend se necessário).
#
# Uso:
#   source .aws-deploy/last-ec2.env
#   ./scripts/operador-aplicar-env-ec2.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

COMERCIAL_HOST="${COMERCIAL_HOST:-}"
COMERCIAL_KEY="${COMERCIAL_KEY:-$HOME/.ssh/comercial-cloud-deploy.pem}"
COMERCIAL_USER="${COMERCIAL_USER:-ec2-user}"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/comercial-cloud/comercial-cloud}"
ENV_FILE="${ENV_FILE:-$ROOT/.env.aws-pilot}"

if [[ -z "$COMERCIAL_HOST" && -f "$ROOT/.aws-deploy/last-ec2.env" ]]; then
  # shellcheck disable=SC1091
  source "$ROOT/.aws-deploy/last-ec2.env"
  COMERCIAL_HOST="${PUBLIC_IP:-}"
  COMERCIAL_KEY="${KEY_FILE:-$COMERCIAL_KEY}"
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Arquivo não encontrado: $ENV_FILE" >&2
  echo "Copie .env.aws-pilot.example e preencha as credenciais." >&2
  exit 1
fi

if [[ -z "$COMERCIAL_HOST" ]]; then
  echo "Defina COMERCIAL_HOST ou crie .aws-deploy/last-ec2.env" >&2
  exit 1
fi

echo "▸ Enviando $ENV_FILE → EC2..."
scp -i "$COMERCIAL_KEY" -o StrictHostKeyChecking=accept-new \
  "$ENV_FILE" "${COMERCIAL_USER}@${COMERCIAL_HOST}:${DEPLOY_DIR}/.env.aws-pilot"

SSH=(ssh -i "$COMERCIAL_KEY" -o StrictHostKeyChecking=accept-new "${COMERCIAL_USER}@${COMERCIAL_HOST}")

echo "▸ Reiniciando backend (e frontend se URLs mudaram)..."
"${SSH[@]}" bash <<REMOTE
set -euo pipefail
cd ${DEPLOY_DIR}
COMPOSE="\$(command -v docker-compose >/dev/null 2>&1 && echo docker-compose || echo 'docker compose')"
CF="--env-file .env.aws-pilot -f docker-compose.prod.yml"
DOMAIN="\$(grep '^DOMAIN=' .env.aws-pilot | cut -d= -f2- || true)"
PROJECT="\$(basename "\$(pwd)")"
CERT_VOL="\${PROJECT}_certbot_conf"
if [[ -n "\$DOMAIN" ]] && sudo docker volume inspect "\$CERT_VOL" >/dev/null 2>&1; then
  if sudo docker run --rm -v "\${CERT_VOL}:/etc/letsencrypt:ro" alpine \
    test -f "/etc/letsencrypt/live/\${DOMAIN}/fullchain.pem" 2>/dev/null; then
    CF="\${CF} -f docker-compose.prod.tls.yml"
  fi
fi
sudo \$COMPOSE \$CF up -d --force-recreate backend frontend
sleep 20
curl -sf http://127.0.0.1/health && echo " OK" || echo " aguardando health..."
REMOTE

echo "✔ Env aplicado na EC2"
