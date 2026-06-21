# Operador — passo a passo (ordem recomendada)

Piloto atual: **https://56-124-36-136.sslip.io**  
Scripts auxiliares: `./scripts/operador-passo.sh <etapa>`

Marque cada bloco em `docs/PENDENCIAS-OPERADOR.md` ao concluir.

---

## 1. Asaas (billing + PIX PDV)

### 1.1 Conta e API key

1. Acesse [Asaas](https://www.asaas.com/) → **Minha conta → Integrações → API**.
2. Copie a **API Key** (comece pelo **sandbox** se ainda estiver validando).
3. No seu `.env.aws-pilot` (local, nunca commitar):

```bash
COMERCIAL_CLOUD_BILLING_GATEWAY=asaas
COMERCIAL_CLOUD_BILLING_ASAAS_ENABLED=true
COMERCIAL_CLOUD_BILLING_ASAAS_BASE_URL=https://api-sandbox.asaas.com/v3
# Produção: https://api.asaas.com/v3
ASAAS_API_KEY=sua_chave_aqui
ASAAS_WEBHOOK_TOKEN=$(openssl rand -hex 24)   # gere uma vez e guarde
```

### 1.2 Webhook no painel Asaas

| Campo | Valor (piloto) |
|-------|----------------|
| URL | `https://56-124-36-136.sslip.io/api/v1/public/webhooks/asaas` |
| Método | POST |
| Eventos | Pagamento confirmado / recebido |
| Token (se o Asaas pedir) | mesmo valor de `ASAAS_WEBHOOK_TOKEN` |

O backend valida o campo `token` do JSON quando `ASAAS_WEBHOOK_TOKEN` está definido.

### 1.3 Aplicar na EC2

```bash
source .aws-deploy/last-ec2.env
./scripts/operador-passo.sh asaas-apply
./scripts/operador-passo.sh verify
```

### 1.4 Teste manual

1. Retaguarda → **Assinatura** → gerar fatura / checkout.
2. Pagar no sandbox Asaas (PIX ou cartão teste).
3. Confirmar webhook no painel Asaas (HTTP 200).
4. Tenant deve voltar a **ATIVA** (sem erro 402).

---

## 2. Focus NFe (NFC-e)

### 2.1 Credenciais

1. Cadastro em [Focus NFe](https://focusnfe.com.br/).
2. Token de homologação no painel.
3. Na retaguarda → **Fiscal** → configurar CNPJ, IE, CSC, série e ambiente **HOMOLOGACAO**.

```bash
COMERCIAL_CLOUD_FISCAL_PROVIDER=focusnfe
COMERCIAL_CLOUD_FISCAL_FOCUSNFE_ENABLED=true
FOCUSNFE_TOKEN=seu_token_focus
```

### 2.2 Aplicar e testar

```bash
./scripts/operador-passo.sh focus-apply
```

1. Finalize uma venda no PDV.
2. **Fiscal** → processar documento.
3. Status **AUTORIZADO** em homologação antes de mudar para **PRODUCAO**.

---

## 3. Keycloak (auth produção)

Keycloak já roda em `/auth` no piloto. Falta alinhar realm e provisioning:

```bash
source .aws-deploy/last-ec2.env
./scripts/generate-keycloak-realm-prod.sh https://56-124-36-136.sslip.io
./scripts/operador-passo.sh keycloak-apply
```

Variáveis (já no `.env.aws-pilot` após deploy):

```bash
COMERCIAL_CLOUD_KEYCLOAK_PROVISIONING_ENABLED=true
KEYCLOAK_PUBLIC_URL=https://56-124-36-136.sslip.io/auth
KEYCLOAK_ISSUER_URL=https://56-124-36-136.sslip.io/auth/realms/comercialcloud
```

**Checklist painel Keycloak** (`/auth/admin`):

- [ ] Realm `comercialcloud` importado/atualizado
- [ ] Client `comercial-cloud-web` com redirect `https://56-124-36-136.sslip.io/*`
- [ ] Client `comercial-cloud-api` para backend OIDC
- [ ] Mapper `tenant_id` / `app_user_id` nos tokens (ver `infra/keycloak/`)

Teste: criar conta em `/cadastro` com senha → login em `/login` com e-mail real.

---

## 4. E-mail transacional (SMTP)

Exemplo Amazon SES (sa-east-1):

```bash
COMERCIAL_CLOUD_MAIL_ENABLED=true
COMERCIAL_CLOUD_MAIL_APP_URL=https://56-124-36-136.sslip.io
MAIL_HOST=email-smtp.sa-east-1.amazonaws.com
MAIL_PORT=587
MAIL_STARTTLS=REQUIRED
MAIL_USER=AKIA...
MAIL_PASSWORD=...
MAIL_FROM=noreply@seudominio.com.br
```

```bash
./scripts/operador-passo.sh mail-apply
```

Teste: **Usuários → Convidar** e verificar recebimento na caixa real.

---

## 5. DNS definitivo

### Opção A — continuar sslip.io (piloto)

```bash
source .aws-deploy/last-ec2.env
./scripts/aws-setup-dns-auto.sh
```

### Opção B — domínio próprio

1. Registre `app.seudominio.com.br` (A → IP `56.124.36.136`).
2. Atualize `.env.aws-pilot`:

```bash
DOMAIN=app.seudominio.com.br
APP_URL=https://app.seudominio.com.br
KEYCLOAK_PUBLIC_URL=https://app.seudominio.com.br/auth
KEYCLOAK_ISSUER_URL=https://app.seudominio.com.br/auth/realms/comercialcloud
```

3. Regere realm + TLS:

```bash
./scripts/generate-keycloak-realm-prod.sh https://app.seudominio.com.br
CERTBOT_EMAIL=seu@email.com ./scripts/aws-setup-tls-ec2.sh
SCHEME=https ./scripts/aws-deploy-comercial-ec2.sh
```

4. Atualize webhook Asaas e redirect URIs Keycloak com o novo domínio.

---

## 6. Backup PostgreSQL (cron EC2)

```bash
source .aws-deploy/last-ec2.env
./scripts/install-backup-cron-ec2.sh
```

Backups em `/opt/comercial-cloud/backups/` (retenção 14 dias).  
Restaurar: `gunzip -c backup.sql.gz | docker exec -i <postgres> psql -U comercialcloud comercialcloud`

---

## 7. Jurídico e comercial

- [ ] Advogado revisa `/termos` e `/privacidade` (substituir placeholders se necessário)
- [ ] Contrato piloto white-label: `docs/PILOTO-WHITELABEL.md`
- [ ] Landing Kiwify: e-mail e checkout definitivos
- [ ] Política de retenção de dados / LGPD no contrato

Template de checklist: `docs/CHECKLIST-JURIDICO.md`

---

## Verificação rápida

```bash
source .aws-deploy/last-ec2.env
APP_URL=https://56-124-36-136.sslip.io API_URL=https://56-124-36-136.sslip.io ./scripts/verify-operador-piloto.sh
```
