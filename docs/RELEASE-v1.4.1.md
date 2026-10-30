# Release v1.4.1 — ComercialCloud

**Data:** 2026-10-30

CORS do backend efetivo em produção e em desenvolvimento.

---

## Alterações

### Backend
- O `application.yml` nunca era carregado (o projeto não tem a extensão `quarkus-config-yaml`), então o CORS declarado nele não valia e o frontend servido em outra origem recebia bloqueio do navegador ao chamar a API.
- A configuração de CORS passou para `application.properties`, com origens em `QUARKUS_HTTP_CORS_ORIGINS` (padrão `http://localhost:3000`) e os cabeçalhos `x-tenant-id`, `x-user-id` e `x-correlation-id` liberados.

### Verificação
- Jornada completa em navegador (produto → caixa → venda → pagamento → finalização → relatório) contra a stack Compose isolada com o frontend em origem distinta do backend.
