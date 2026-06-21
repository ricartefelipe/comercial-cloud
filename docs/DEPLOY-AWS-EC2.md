# Deploy na AWS (EC2 dedicada) — ComercialCloud

Piloto e produção inicial usando **EC2 + Docker Compose + Nginx + Keycloak OIDC**, no mesmo padrão do Fluxe B2B Suite (`fluxe-b2b-suite/docs/DEPLOY-AWS-EC2.md` no monorepo).

**Instância separada** do Fluxe — key pair e security group próprios.

---

## Arquitetura (um host)

| Rota | Serviço |
|------|---------|
| `/` | Frontend Next.js |
| `/api/` | Backend Quarkus |
| `/auth/` | Keycloak 26 (OIDC) |
| `/health` | Health check backend |

Tudo no mesmo domínio piloto, ex.: `https://54-94-52-89.sslip.io`.

---

## Pré-requisitos

| Item | Detalhe |
|------|---------|
| AWS CLI | Configurado (`aws configure`) |
| SSH | Acesso à região `sa-east-1` (ou `AWS_REGION`) |
| GitHub | Repo `comercial-cloud` clonado localmente |

---

## Passo a passo (piloto)

### 1. Provisionar EC2 nova

```bash
cd comercial-cloud
chmod +x scripts/*.sh
./scripts/aws-provision-comercial-ec2.sh
```

Cria:

- EC2 `t3.medium`, Amazon Linux 2023, 60 GB
- Key pair `~/.ssh/comercial-cloud-deploy.pem`
- Security group `comercial-cloud-prod-sg` (22 do seu IP, 80/443 público)
- Metadata em `.aws-deploy/last-ec2.env` (**não commitar**)

### 2. Validar DNS piloto (sslip.io)

```bash
source .aws-deploy/last-ec2.env
./scripts/aws-setup-dns-auto.sh
```

### 3. Deploy da stack (Keycloak + OIDC ativo)

```bash
./scripts/aws-deploy-comercial-ec2.sh
```

Gera `.env.aws-pilot`, realm Keycloak com URLs corretas, build Docker na EC2 (~15–25 min).

### 4. HTTPS (Let's Encrypt)

```bash
CERTBOT_EMAIL=seu@email.com ./scripts/aws-setup-tls-ec2.sh
```

Atualiza `.env.aws-pilot` para HTTPS, regenera realm e recria backend/frontend/keycloak.

### 5. Validar

```bash
source .aws-deploy/last-ec2.env
curl -sf "https://${PILOT_DOMAIN}/health"
curl -sf "https://${PILOT_DOMAIN}/api/v1/public/planos"
```

Abrir no browser:

- App: `https://<PILOT_DOMAIN>/login`
- Keycloak admin: `https://<PILOT_DOMAIN>/auth/admin` (user `admin`, senha em `.env.aws-pilot`)

**Login demo (realm importado):**

| E-mail | Senha | Perfil |
|--------|-------|--------|
| `admin@demo.com` | `admin123` | ADMIN |
| `caixa@demo.com` | `caixa123` | CAIXA |

---

## Keycloak (igual Fluxe prod)

- Imagem `quay.io/keycloak/keycloak:26.0`
- Postgres dedicado (`keycloak` DB)
- `KC_HTTP_RELATIVE_PATH=/auth` + Nginx reverse proxy
- Realm `comercialcloud` com clients `comercial-cloud-web` e `comercial-cloud-api`
- Claims JWT: `tenant_id`, `app_user_id`, `loja_id`
- Provisioning pós-onboarding/convite: `COMERCIAL_CLOUD_KEYCLOAK_PROVISIONING_ENABLED=true`

Gerar realm para outro domínio:

```bash
./scripts/generate-keycloak-realm-prod.sh https://app.seudominio.com.br
```

Template: `infra/keycloak/realm-prod-template.json`.

---

## Variáveis principais (`.env.aws-pilot`)

| Variável | Exemplo |
|----------|---------|
| `DOMAIN` | `54-94-52-89.sslip.io` |
| `APP_URL` | `https://54-94-52-89.sslip.io` |
| `KEYCLOAK_PUBLIC_URL` | `https://54-94-52-89.sslip.io/auth` |
| `KEYCLOAK_ISSUER_URL` | `https://.../auth/realms/comercialcloud` |
| `POSTGRES_PASSWORD` | gerado no deploy |
| `KEYCLOAK_ADMIN_PASSWORD` | gerado no deploy |

Ver `.env.aws-pilot.example` e [`PENDENCIAS-OPERADOR.md`](./PENDENCIAS-OPERADOR.md) para Asaas, Focus, SES.

---

## Domínio próprio (depois do piloto)

1. Route 53 ou Registro.br → A record para Elastic IP da EC2
2. Atualizar `.env.aws-pilot` com domínio real
3. `./scripts/generate-keycloak-realm-prod.sh https://app.seudominio.com.br`
4. `CERTBOT_EMAIL=... COMERCIAL_DOMAIN=app.seudominio.com.br ./scripts/aws-setup-tls-ec2.sh`
5. Redeploy: `./scripts/aws-deploy-comercial-ec2.sh`

---

## Comandos úteis na EC2

```bash
ssh -i ~/.ssh/comercial-cloud-deploy.pem ec2-user@<IP>
cd /opt/comercial-cloud/comercial-cloud
sudo docker-compose --env-file .env.aws-pilot -f docker-compose.prod.yml ps
sudo docker-compose --env-file .env.aws-pilot -f docker-compose.prod.yml logs -f backend
./scripts/backup-postgres.sh
```

---

## Referências

- [`DEPLOY-PRODUCAO.md`](./DEPLOY-PRODUCAO.md) — variáveis Asaas/Focus/mail
- [`PENDENCIAS-OPERADOR.md`](./PENDENCIAS-OPERADOR.md) — checklist operacional
- Fluxe AWS: `../fluxe-b2b-suite/docs/DEPLOY-AWS-EC2.md`
