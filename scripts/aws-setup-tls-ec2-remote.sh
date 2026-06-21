#!/usr/bin/env bash
# Executado na EC2 por aws-setup-tls-ec2.sh.
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/opt/comercial-cloud/comercial-cloud}"
cd "$DEPLOY_DIR"

COMERCIAL_DOMAIN="${COMERCIAL_DOMAIN:?COMERCIAL_DOMAIN obrigatório}"
CERTBOT_EMAIL="${CERTBOT_EMAIL:-}"

COMPOSE="sudo docker-compose"
CF="--env-file .env.aws-pilot -f docker-compose.prod.yml -f docker-compose.prod.tls.yml"

cp -f deploy/nginx/conf.d/default-pilot.conf deploy/nginx/conf.d/default-pilot-active.conf

$COMPOSE $CF up -d nginx
sleep 3

EMAIL_ARG=""
if [[ -n "$CERTBOT_EMAIL" ]]; then
  EMAIL_ARG="--email $CERTBOT_EMAIL"
else
  EMAIL_ARG="--register-unsafely-without-email"
fi

$COMPOSE $CF run --rm --entrypoint certbot certbot certonly --webroot -w /var/www/certbot \
  $EMAIL_ARG --agree-tos --no-eff-email \
  -d "$COMERCIAL_DOMAIN"

$COMPOSE $CF run --rm --entrypoint sh certbot -c "chmod 644 /etc/letsencrypt/archive/${COMERCIAL_DOMAIN}/privkey1.pem"

sed "s/__COMERCIAL_DOMAIN__/${COMERCIAL_DOMAIN}/g" \
  deploy/nginx/conf.d/default-pilot-ssl.conf > deploy/nginx/conf.d/default-pilot-active.conf

$COMPOSE $CF up -d --force-recreate nginx certbot-renew
sleep 5

$COMPOSE $CF up -d --force-recreate backend frontend keycloak
sleep 10

curl -sf "https://${COMERCIAL_DOMAIN}/api/v1/public/planos" >/dev/null && echo " TLS OK"
