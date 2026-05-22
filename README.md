# ComercialCloud

Plataforma SaaS multitenant para **PDV (ponto de venda)** e **retaguarda web**, voltada a pequenos e médios comércios.

## Visão

- Vendas no balcão via PDV web (busca rápida, carrinho, pagamentos, comprovante)
- Retaguarda administrativa (produtos, estoque, clientes, vendas, caixa, financeiro, dashboard)
- Isolamento lógico por tenant (`X-Tenant-Id` ou claim JWT `tenant_id`)
- Autenticação via Keycloak (OIDC/JWT) ou modo demo local para desenvolvimento

## Stack

| Camada | Tecnologias |
|--------|-------------|
| Backend | Java 21, Quarkus 3.17, RESTEasy Reactive, Hibernate Panache, PostgreSQL, Liquibase YAML, Bean Validation, OpenAPI |
| Frontend | Next.js 14, TypeScript, TailwindCSS, React Hook Form, Zod, TanStack Query |
| Infra | Docker Compose (PostgreSQL, backend, frontend; Keycloak opcional) |

## Arquitetura backend

Clean Architecture pragmática:

```
backend/src/main/java/com/comercialcloud/
├── domain/           # enums e exceções de domínio (sem JPA)
├── application/      # serviços de aplicação (orquestram regras)
├── infrastructure/   # entidades Panache, segurança, auditoria, observabilidade
├── interfaces/rest/  # resources REST + DTOs inline (records)
└── config/           # OpenAPI
```

**Decisões:**
- Multitenancy por coluna `tenant_id` + filtro HTTP (`TenantContext`, `TenantRequestFilter`)
- API versionada em `/api/v1/*`
- RBAC por role (`ADMIN`, `GERENTE`, `CAIXA`) resolvida a partir do usuário autenticado
- Onboarding de tenant via `POST /api/v1/public/onboarding`
- Liquibase controla schema; Hibernate `database.generation=none` em produção

## Executar localmente

### Pré-requisitos

- Docker e Docker Compose
- Java 21 e Maven (ou use `./mvnw`)
- Node.js 20+

### 1. Subir infraestrutura

```bash
docker compose up -d
```

Serviços:
- PostgreSQL: `localhost:5432` (db/user/pass: `comercialcloud`)
- Backend: `http://localhost:8080`
- Frontend: `http://localhost:3000`
- Keycloak (opcional): `docker compose --profile keycloak up -d` → `http://localhost:8180`

### 2. Backend (dev)

```bash
cd backend
./mvnw quarkus:dev
```

Migrations Liquibase rodam automaticamente na subida (`migrate-at-start: true`).

### 3. Frontend (dev)

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

Acesse:
- Login: `http://localhost:3000/login` (Keycloak ou **modo demo**)
- Retaguarda: `http://localhost:3000/dashboard`
- PDV: `http://localhost:3000/pdv`

### 4. Keycloak (opcional)

```bash
docker compose --profile keycloak up -d
```

Realm `comercialcloud` importado de `infra/keycloak/realm-comercialcloud.json`.

Usuários demo:
| E-mail | Senha | Role |
|--------|-------|------|
| admin@demo.com | admin123 | ADMIN |
| caixa@demo.com | caixa123 | CAIXA |

Backend com JWT: `./mvnw quarkus:dev -Dquarkus.profile=auth`

### 5. Testes backend

```bash
cd backend
./mvnw clean -Dtest=ComercialCloudIT test
```

### 6. Testes E2E (Playwright)

```bash
cd frontend && npm run build && npm start &
cd e2e && npm install && npx playwright install chromium && npm test
```

## Swagger / OpenAPI

- UI: [http://localhost:8080/q/swagger-ui](http://localhost:8080/q/swagger-ui)
- OpenAPI JSON: [http://localhost:8080/q/openapi](http://localhost:8080/q/openapi)

## Headers obrigatórios (v1 demo)

| Header | Descrição |
|--------|-----------|
| `X-Tenant-Id` | UUID do tenant (obrigatório em rotas de dados) |
| `X-User-Id` | UUID do operador (obrigatório para caixa/vendas) |
| `X-Correlation-Id` | Opcional; ecoado na resposta para rastreio |

## Dados demo (seed Liquibase)

| Entidade | UUID | Descrição |
|----------|------|-----------|
| Tenant | `11111111-1111-1111-1111-111111111111` | Demo Tenant |
| Loja | `22222222-2222-2222-2222-222222222222` | Loja Demo |
| Admin | `33333333-3333-3333-3333-333333333333` | admin@demo.com |
| Operador caixa | `44444444-4444-4444-4444-444444444444` | caixa@demo.com |
| Produto 1 | `55555555-5555-5555-5555-555555555551` | R$ 10,00 — estoque 100 |
| Tenant 2 | `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa` | Isolamento multitenant |

O frontend usa esses IDs em `frontend/src/lib/constants.ts`.

## Fluxo vertical principal

1. Cadastrar produto (`POST /api/v1/produtos`)
2. Ajustar estoque (`POST /api/v1/estoques/ajustes`)
3. Abrir caixa (`POST /api/v1/caixas/abrir`)
4. Criar venda PDV (`POST /api/v1/vendas`)
5. Adicionar itens e pagamentos
6. Finalizar venda (`POST /api/v1/vendas/{id}/finalizar`) → baixa estoque
7. Consultar na retaguarda (`GET /api/v1/vendas/{id}`)
8. Fechar caixa (`POST /api/v1/caixas/{id}/fechar`)
9. Dashboard (`GET /api/v1/dashboard/resumo`)

Exemplos completos: [`api-examples.http`](api-examples.http)

## Migrations

Changelog master: `backend/src/main/resources/db/changelog/db.changelog-master.yaml`

Domínios separados: tenant, loja, usuario, produto, estoque, movimentacao, cliente, caixa, venda, financeiro, auditoria, seed.

## Estrutura do monorepo

```
comercial-cloud/
├── backend/          # API Quarkus
├── frontend/         # Next.js (retaguarda + PDV)
├── docker-compose.yml
├── api-examples.http
└── README.md
```

## Observabilidade

- Health: `GET /health`
- Métricas Prometheus: `GET /metrics`
- `X-Correlation-Id` por requisição (aceito e retornado)
- Logs com tenant e correlation id
- Erros padronizados: `timestamp`, `status`, `error`, `message`, `path`, `correlationId`, `code`, `details`

## Fluxo de branches (GitFlow)

| Branch | Uso |
|--------|-----|
| `main` | Produção estável; recebe merges de `develop` via release |
| `develop` | Integração contínua; base para novas funcionalidades |
| `feature/*` | Novas funcionalidades → merge em `develop` |
| `hotfix/*` | Correções urgentes em produção → merge em `main` e `develop` |
| `release/*` | Preparação de versão (`develop` → `main`) |

Releases são publicadas a partir de `main` com tags semver (`v1.0.0`, `v1.1.0`, …).
