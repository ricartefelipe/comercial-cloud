#!/usr/bin/env bash
# DNS automático piloto EC2 — sslip.io (ex.: 54-94-52-89.sslip.io → IP).
#
# Uso:
#   COMERCIAL_HOST=54.94.52.89 ./scripts/aws-setup-dns-auto.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

COMERCIAL_HOST="${COMERCIAL_HOST:-}"
if [[ -z "$COMERCIAL_HOST" && -f "$ROOT/.aws-deploy/last-ec2.env" ]]; then
  # shellcheck disable=SC1091
  source "$ROOT/.aws-deploy/last-ec2.env"
  COMERCIAL_HOST="${PUBLIC_IP:-}"
fi

if [[ -z "$COMERCIAL_HOST" ]]; then
  echo "Defina COMERCIAL_HOST (IP público da EC2)." >&2
  exit 1
fi

COMERCIAL_DOMAIN="${COMERCIAL_DOMAIN:-${COMERCIAL_HOST//./-}.sslip.io}"

echo "▸ Domínio piloto: $COMERCIAL_DOMAIN → $COMERCIAL_HOST"

for i in $(seq 1 15); do
  RESOLVED="$(dig +short "$COMERCIAL_DOMAIN" A 2>/dev/null | head -1 || true)"
  if [[ "$RESOLVED" == "$COMERCIAL_HOST" ]]; then
    echo "✔ DNS OK"
    echo "COMERCIAL_DOMAIN=$COMERCIAL_DOMAIN"
    exit 0
  fi
  [[ "$i" -eq 1 ]] && echo "▸ Aguardando DNS..."
  sleep 2
done

echo "DNS não resolveu (esperado $COMERCIAL_HOST, obtido ${RESOLVED:-vazio})." >&2
exit 1
