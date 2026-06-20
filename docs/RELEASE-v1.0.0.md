# Release v1.0.0 — ComercialCloud

**Data:** 2026-06-20  
**Branch:** `main` / `develop` @ tag `v1.0.0`

Primeira release operacional para piloto white-label e homologação SaaS.

---

## Destaques

### PDV e retaguarda
- PDV web com busca, carrinho, pagamentos e comprovante
- Retaguarda: produtos, estoque, clientes, vendas, caixa, financeiro, dashboard
- Multitenant por `tenant_id` + RBAC (Admin, Gerente, Caixa)

### Onboarding e billing
- Cadastro self-service (`/cadastro`) com trial 14 dias e seed demo
- Planos Essencial / Pro / Premium
- Cobrança recorrente, faturas, retentativa, suspensão HTTP 402
- Adapter **Asaas** (checkout + webhook) e sandbox para demo

### Fiscal
- NFC-e com outbox assíncrona e provedor plugável
- Adapter **Focus NFe** para emissão/cancelamento real (homologação/produção)
- UI `/fiscal` para configuração por loja

### Auth
- Keycloak opcional (perfil `auth`)
- Protocol mappers JWT (`tenant_id`, `app_user_id`, `loja_id`)
- Provisioning automático de usuário admin pós-onboarding

### Entrega comercial
- `README-COMERCIAL.md`, `LICENSE`, `scripts/package-dist.sh`
- CI: backend, frontend, e2e smoke
- Testes: `ComercialCloudIT`, `FiscalBillingIT`

---

## Migrações de banco (Liquibase)

016–021: configuração fiscal, documentos, planos, assinaturas, faturas, checkout gateway.

---

## Breaking changes

Nenhum — primeira release taggeada.

---

## Pós-release (operacional)

Antes de vender SaaS direto ao lojista:

- Credenciais Asaas e Focus NFe em produção
- Keycloak + HTTPS + backup Postgres
- E-mails transacionais e documentos LGPD na landing
- Ver [`DEPLOY-PRODUCAO.md`](./DEPLOY-PRODUCAO.md)

---

## Como atualizar a partir de uma versão anterior

```bash
git fetch --tags
git checkout v1.0.0
docker compose up -d --build
# Liquibase aplica migrations automaticamente na subida do backend
```
