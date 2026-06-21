#!/usr/bin/env bash
# Instala cron diário de backup PostgreSQL na EC2 piloto.
#
# Uso:
#   source .aws-deploy/last-ec2.env
#   ./scripts/install-backup-cron-ec2.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

COMERCIAL_HOST="${COMERCIAL_HOST:-}"
COMERCIAL_KEY="${COMERCIAL_KEY:-$HOME/.ssh/comercial-cloud-deploy.pem}"
COMERCIAL_USER="${COMERCIAL_USER:-ec2-user}"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/comercial-cloud/comercial-cloud}"
CRON_HOUR="${CRON_HOUR:-3}"

if [[ -z "$COMERCIAL_HOST" && -f "$ROOT/.aws-deploy/last-ec2.env" ]]; then
  # shellcheck disable=SC1091
  source "$ROOT/.aws-deploy/last-ec2.env"
  COMERCIAL_HOST="${PUBLIC_IP:-}"
  COMERCIAL_KEY="${KEY_FILE:-$COMERCIAL_KEY}"
fi

if [[ -z "$COMERCIAL_HOST" ]]; then
  echo "Defina COMERCIAL_HOST ou .aws-deploy/last-ec2.env" >&2
  exit 1
fi

SSH=(ssh -i "$COMERCIAL_KEY" -o StrictHostKeyChecking=accept-new "${COMERCIAL_USER}@${COMERCIAL_HOST}")

echo "▸ Configurando backup diário (${CRON_HOUR}:00) na EC2..."
"${SSH[@]}" bash <<REMOTE
set -eo pipefail
if ! command -v crontab >/dev/null 2>&1; then
  sudo dnf install -y cronie >/dev/null 2>&1 || sudo yum install -y cronie >/dev/null 2>&1 || true
  sudo systemctl enable --now crond 2>/dev/null || sudo systemctl enable --now cronie 2>/dev/null || true
fi
mkdir -p /opt/comercial-cloud/backups
(crontab -l 2>/dev/null | grep -v 'backup-postgres.sh' || true; echo "0 ${CRON_HOUR} * * * cd ${DEPLOY_DIR} && ./scripts/backup-postgres.sh /opt/comercial-cloud/backups >> /var/log/comercialcloud-backup.log 2>&1") | crontab -
chmod +x ${DEPLOY_DIR}/scripts/backup-postgres.sh
echo "▸ Teste imediato do backup..."
cd ${DEPLOY_DIR}
./scripts/backup-postgres.sh /opt/comercial-cloud/backups
ls -lh /opt/comercial-cloud/backups | tail -3
REMOTE

echo "✔ Cron de backup instalado"
