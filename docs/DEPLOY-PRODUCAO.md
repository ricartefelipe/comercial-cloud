# Deploy em produção — ComercialCloud v1.0.0

Guia operacional para homologação e produção de um piloto **white-label** ou SaaS. Pressupõe Docker (ou VM Linux) com PostgreSQL, backend Quarkus, frontend Next.js e Keycloak.

---

## 1. Pré-requisitos

| Item | Detalhe |
|------|---------|
| Domínio | Ex.: `app.seudominio.com.br` (frontend), `api.seudominio.com.br` (backend), `auth.seudominio.com.br` (Keycloak) |
| HTTPS | Certificado TLS (Let's Encrypt / reverse proxy) |
| PostgreSQL 16 | Banco dedicado; backup diário configurado |
| Contas externas | Asaas (billing), Focus NFe (NFC-e), opcional SMTP |

---

## 2. Subir a stack base

```bash
git clone git@github.com:ricartefelipe/comercial-cloud.git
cd comercial-cloud
git checkout v1.0.0   # ou main/develop após release

docker compose up -d postgres backend frontend
docker compose --profile keycloak up -d keycloak
```

Verifique:

- Backend health: `GET https://api.seudominio.com.br/health`
- Frontend: `https://app.seudominio.com.br/login`
- Keycloak: `https://auth.seudominio.com.br` (admin importa realm de `infra/keycloak/realm-comercialcloud.json`)

---

## 3. Variáveis de ambiente (backend)

Copie `backend/.env.example` e configure no container ou `application.properties` overlay.

### 3.1 Banco e HTTP

```bash
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://postgres:5432/comercialcloud
QUARKUS_DATASOURCE_USERNAME=comercialcloud
QUARKUS_DATASOURCE_PASSWORD=<senha-forte>
QUARKUS_HTTP_PORT=8080
```

### 3.2 CORS (frontend em produção)

Em `application.yml` ou variável equivalente, inclua a origem do frontend:

```yaml
quarkus:
  http:
    cors:
      origins: "https://app.seudominio.com.br"
```

### 3.3 Perfil auth (Keycloak)

Suba o backend com perfil `auth`:

```bash
QUARKUS_PROFILE=auth
QUARKUS_OIDC_AUTH_SERVER_URL=https://auth.seudominio.com.br/realms/comercialcloud
QUARKUS_OIDC_CLIENT_ID=comercial-cloud-api
```

### 3.4 Billing — Asaas

**Homologação (sandbox):**

```yaml
comercial-cloud:
  billing:
    gateway: asaas
    asaas:
      enabled: true
      api-key: ${ASAAS_API_KEY}
      base-url: https://api-sandbox.asaas.com/v3
      webhook-token: ${ASAAS_WEBHOOK_TOKEN}
```

**Produção:**

```yaml
      base-url: https://api.asaas.com/v3
```

Variáveis:

```bash
COMERCIAL_CLOUD_BILLING_ASAAS_ENABLED=true
ASAAS_API_KEY=<chave_asaas>
ASAAS_WEBHOOK_TOKEN=<token_secreto_webhook>
```

**Webhook no painel Asaas:**

- URL: `POST https://api.seudominio.com.br/api/v1/public/webhooks/asaas`
- Eventos: pagamento confirmado / recebido
- O campo `token` do payload deve coincidir com `ASAAS_WEBHOOK_TOKEN` (se configurado)

### 3.5 Fiscal — Focus NFe

**Homologação primeiro:**

```yaml
comercial-cloud:
  fiscal:
    provider: focusnfe
    focusnfe:
      enabled: true
      token: ${FOCUSNFE_TOKEN}
```

```bash
COMERCIAL_CLOUD_FISCAL_FOCUSNFE_ENABLED=true
FOCUSNFE_TOKEN=<token_focus_homologacao>
```

No painel Focus NFe: cadastre a empresa emitente (CNPJ, IE, certificado). Na tela `/fiscal` do ComercialCloud, configure ambiente **HOMOLOGACAO**, série, CSC e regime tributário.

**Produção:** troque o token para produção e defina ambiente **PRODUCAO** na config fiscal da loja.

### 3.6 Keycloak — provisioning pós-onboarding

```yaml
comercial-cloud:
  keycloak:
    provisioning:
      enabled: true
    server-url: https://auth.seudominio.com.br
    realm: comercialcloud
    admin-client-id: comercial-cloud-api
    admin-client-secret: <secret_do_cliente_service_account>
```

```bash
COMERCIAL_CLOUD_KEYCLOAK_PROVISIONING_ENABLED=true
```

O realm importado já inclui mappers JWT: `tenant_id`, `app_user_id`, `loja_id`.

---

## 4. Variáveis de ambiente (frontend)

Arquivo `frontend/.env.production` (ou build args Docker):

```bash
NEXT_PUBLIC_API_URL=https://api.seudominio.com.br
NEXT_PUBLIC_KEYCLOAK_URL=https://auth.seudominio.com.br
NEXT_PUBLIC_KEYCLOAK_REALM=comercialcloud
NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=comercial-cloud-web
```

Rebuild do frontend após alterar variáveis `NEXT_PUBLIC_*`.

---

## 5. Checklist de homologação (antes de 1ª loja real)

Execute nesta ordem:

### Billing

1. Criar conta trial em `/cadastro`
2. Gerar fatura (scheduler ou endpoint admin)
3. Pagar via checkout Asaas sandbox
4. Confirmar webhook → tenant ativo (sem HTTP 402)
5. Simular inadimplência → tenant suspenso → pagar → reativado

### Fiscal

1. Configurar loja em `/fiscal` (homologação)
2. Realizar venda no PDV e emitir NFC-e
3. Verificar documento **AUTORIZADO** e URL DANFE
4. Cancelar com justificativa ≥ 15 caracteres

### Auth

1. Onboarding com senha → usuário criado no Keycloak
2. Login OIDC → JWT com `tenant_id` correto
3. PDV e retaguarda acessíveis com perfil Admin

---

## 6. Reverse proxy (exemplo Nginx)

```nginx
# api.seudominio.com.br → backend:8080
server {
  listen 443 ssl;
  server_name api.seudominio.com.br;
  location / {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
}

# app.seudominio.com.br → frontend:3000
server {
  listen 443 ssl;
  server_name app.seudominio.com.br;
  location / {
    proxy_pass http://127.0.0.1:3000;
    proxy_set_header Host $host;
  }
}
```

---

## 7. Backup e monitoramento

| Item | Recomendação |
|------|--------------|
| Postgres | `pg_dump` diário; teste de restore mensal |
| Métricas | Scrape `GET /metrics` (Prometheus) |
| Health | Alerta se `/health` falhar por > 2 min |
| Logs | Centralizar stdout dos containers |

---

## 8. Landing + trial

No repositório `comercial-cloud-landing`, configure antes de divulgar:

```javascript
window.COMERCIALCLOUD_CONFIG = {
  trialUrl: 'https://app.seudominio.com.br/cadastro',
  contactEmail: 'contato@seudominio.com.br'
};
```

---

## 9. Piloto white-label (roteiro sugerido)

1. Entregar zip comercial (`scripts/package-dist.sh`) + este guia
2. Homologação conjunta (checklist §5) com o parceiro
3. Branding do parceiro (logo/cores) — documentar no contrato
4. Go-live produção Asaas + Focus + Keycloak
5. Suporte 1º nível pelo parceiro; 2º nível sua equipe

---

## 8. E-mail transacional

Ative SMTP para boas-vindas, fatura, suspensão e confirmação de pagamento:

```yaml
comercial-cloud:
  mail:
    enabled: true
    from: ${MAIL_FROM}
    app-url: ${APP_URL}
```

```bash
MAIL_HOST=smtp.seudominio.com.br
MAIL_PORT=587
MAIL_STARTTLS=REQUIRED
```

**Homologação com MailHog:**

```bash
docker compose -f docker-compose.yml -f docker-compose.homolog.yml up -d
# UI: http://localhost:8025
```

---

## 9. Backup

```bash
./scripts/backup-postgres.sh
# Agende cron diário; retenção 14 dias automática
```

---

## 10. Verificação rápida

```bash
./scripts/verify-homologacao.sh
```

---

## 11. Piloto white-label

Roteiro completo: [`PILOTO-WHITELABEL.md`](./PILOTO-WHITELABEL.md)

---

## Referências

- [`README-COMERCIAL.md`](../README-COMERCIAL.md) — visão comercial e adapters
- [`README.md`](../README.md) — desenvolvimento local
- [Focus NFe API](https://doc.focusnfe.com.br/)
- [Asaas API](https://docs.asaas.com/)
