# ComercialCloud — PDV + Retaguarda Multitenant (White-label / Micro SaaS)

> Plataforma completa de **ponto de venda web + retaguarda administrativa** para pequenos e médios comércios. Multitenant, com onboarding self-service, billing recorrente e emissão fiscal NFC-e (padrão outbox, provider plugável).
>
> Documentação técnica: [`README.md`](README.md) · Checklist de lançamento: [`comercial-cloud-landing/CHECKLIST-LANCAMENTO.md`](../comercial-cloud-landing/CHECKLIST-LANCAMENTO.md)

---

## Que problema resolve

Lojistas e revendedores de software precisam de um **PDV + gestão** sem reconstruir do zero:

- Vendas no balcão (busca rápida, carrinho, pagamentos, comprovante)
- Retaguarda (produtos, estoque, clientes, caixa, financeiro, dashboard)
- **Multitenancy** real (cada lojista isolado por `tenant_id`)
- **Onboarding self-service** com trial de 14 dias
- **Cobrança recorrente** (planos, faturas, suspensão por inadimplência)
- **NFC-e** com padrão outbox e adapter fiscal substituível

Sem isso, software houses gastam **6–12 meses** montando PDV, tenant, billing e fiscal — ou pagam mensalidades altas de concorrentes saturados.

---

## O que vem no kit

| # | Recurso | Descrição |
|---|---------|-----------|
| 1 | PDV web | Busca de produtos, carrinho, múltiplas formas de pagamento, comprovante |
| 2 | Retaguarda | Produtos, estoque, clientes, vendas, caixa, financeiro, usuários, configurações |
| 3 | Multitenant + RBAC | Isolamento por coluna + filtro HTTP; perfis Admin, Gerente, Caixa |
| 4 | Onboarding | `POST /api/v1/public/onboarding` + tela `/cadastro` com trial e seed demo |
| 5 | Billing | Planos (Essencial/Pro/Premium), assinatura, faturas, retentativa, HTTP 402 |
| 6 | Fiscal NFC-e | Outbox assíncrona, config por loja, documentos rastreáveis (sandbox incluído) |
| 7 | Observabilidade | Health, métricas Prometheus, correlation id, erros padronizados |
| 8 | Testes | Integração backend (`ComercialCloudIT`, `FiscalBillingIT`) + smoke E2E Playwright |
| 9 | Infra | Docker Compose (Postgres, backend Quarkus, frontend Next.js, Keycloak opcional) |

**Stack:** Java 21 · Quarkus 3 · PostgreSQL · Liquibase · Next.js 14 · TypeScript · TailwindCSS · Keycloak (opcional).

---

## Para quem é

- **Software houses / contabilidades** que querem revender PDV com **marca própria** (white-label)
- **Empreendedores técnicos** montando micro SaaS de varejo regional
- **Times** que precisam de base sólida (clean architecture, OpenAPI, migrations) em vez de MVP frágil

**Não é para:** quem busca produto 100% gerenciado sem operar infraestrutura; quem não pretende integrar gateway fiscal/pagamento real antes de vender SaaS direto ao lojista.

---

## Modelos de monetização sugeridos

| Modelo | Faixa indicada | Esforço operacional |
|--------|----------------|---------------------|
| **White-label (licença + implantação)** | R$ 15k–40k setup + manutenção | Baixo (parceiro faz 1º nível) |
| **SaaS direto por loja** | R$ 99–349/mês por plano | Alto (suporte + fiscal + churn) |
| **Implantação + customização** | R$ 5k–15k por loja | Médio |

> Caminho recomendado: **white-label primeiro**, SaaS direto como aposta de prazo mais longo.

---

## Começando

```bash
docker compose up -d
cd frontend && cp .env.example .env.local && npm install && npm run dev
cd backend && ./mvnw quarkus:dev
```

- Retaguarda: `http://localhost:3000/dashboard`
- PDV: `http://localhost:3000/pdv`
- Cadastro trial: `http://localhost:3000/cadastro`
- Swagger: `http://localhost:8080/q/swagger-ui`

Demo rápido: login → **Entrar em modo demo** (tenant seed) ou **Criar conta grátis**.

---

## O que ainda exige integração externa (antes de SaaS real)

Estes pontos estão **mapeados e com interface pronta**, mas usam adapters sandbox até você plugar fornecedores:

| Gap | Adapter atual | Substituir por |
|-----|---------------|----------------|
| Pagamentos | `SandboxGatewayPagamento` | **Asaas** (`billing.gateway=asaas`) ou Iugu, Stripe, Pagar.me |
| NFC-e / SEFAZ | `SandboxFiscalProvider` | **Focus NFe** (`fiscal.provider=focusnfe`) ou PlugNotas/NFe.io |
| Login pós-onboarding | Modo demo + headers | Keycloak com protocol mappers (`tenant_id`, `app_user_id`) |
| E-mails transacionais | `TransacionalEmailService` (SMTP) | SendGrid / SES / MailHog homolog |
| Backup / HTTPS / domínio | — | Infra de produção |

Implemente os adapters reais **sem alterar** a lógica de domínio (`BillingService`, `FiscalService`).

### Ativar Asaas (PIX)

```yaml
comercial-cloud:
  billing:
    gateway: asaas
    asaas:
      enabled: true
      api-key: ${ASAAS_API_KEY}
      base-url: https://api-sandbox.asaas.com/v3  # ou https://api.asaas.com/v3
```

Webhook: configure `POST /api/v1/public/webhooks/asaas` no painel Asaas.

### Ativar Keycloak pós-onboarding

```yaml
comercial-cloud:
  keycloak:
    provisioning:
      enabled: true
```

Importe o realm em `infra/keycloak/realm-comercialcloud.json` (protocol mappers `tenant_id`, `app_user_id`, `loja_id` incluídos).

### Ativar Focus NFe (NFC-e real)

```yaml
comercial-cloud:
  fiscal:
    provider: focusnfe
    focusnfe:
      enabled: true
      token: ${FOCUSNFE_TOKEN}
```

Homologação usa `https://homologacao.focusnfe.com.br` automaticamente quando `ambiente=HOMOLOGACAO` na configuração fiscal da loja. Produção usa `https://api.focusnfe.com.br`.

Cadastre a empresa emitente no painel Focus NFe e informe CSC/série na tela `/fiscal` do ComercialCloud.

---

## Licença

Produto comercial licenciado — **não** é open source. Resumo:

- ✅ Usar, modificar e operar como **SaaS ou white-label** com sua marca
- ✅ Implantar para clientes finais (lojistas) sem limite de lojas nos produtos que você entrega
- ❌ Revender/redistribuir o código-fonte como template concorrente
- ❌ Publicar o repositório como base open source

Termos completos: [`LICENSE`](LICENSE).

---

## Suporte e entrega

- Política de suporte/SLA: [`comercial-cloud-landing/POLITICA-SUPORTE-SLA.md`](../comercial-cloud-landing/POLITICA-SUPORTE-SLA.md)
- Landing comercial: repositório `comercial-cloud-landing` (GitHub Pages)
- Roteiro de demo: [`comercial-cloud-landing/ROTEIRO-DEMO.md`](../comercial-cloud-landing/ROTEIRO-DEMO.md)

---

*ComercialCloud — Felipe Ricarte Magalhães · 2026*
