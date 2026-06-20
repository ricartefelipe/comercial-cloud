# Release v1.1.0 — ComercialCloud

**Data:** 2026-06-20

Release operacional para piloto white-label: e-mails transacionais, homologação e scripts de ops.

---

## Novidades

### E-mail transacional (SMTP)
- `TransacionalEmailService` — boas-vindas, fatura, checkout, suspensão, pagamento confirmado
- Desligado por padrão; MailHog na homologação via `docker-compose.homolog.yml`

### Scripts de operação
- `scripts/backup-postgres.sh` — backup gzip com retenção 14 dias
- `scripts/verify-homologacao.sh` — health check api/app/openapi

### Documentação
- `docs/PILOTO-WHITELABEL.md` — playbook do 1º parceiro
- `docs/DEPLOY-PRODUCAO.md` — seções e-mail, backup e verificação

---

## Upgrade de v1.0.0

```bash
git fetch --tags
git checkout v1.1.0
docker compose up -d --build
# Novas env vars opcionais: COMERCIAL_CLOUD_MAIL_ENABLED, MAIL_*, APP_URL
```
