# Release v1.2.0 — ComercialCloud

**Data:** 2026-06-20

Multi-loja por plano, branding white-label e compose de produção.

---

## Novidades

### Multi-loja (Premium / Pro / Essencial)
- `max_lojas` por plano: Essencial 1, Pro 3, Premium 10
- API `/api/v1/lojas` — listar, criar, ativar/desativar, resumo de limites
- UI `/lojas` + seletor de loja no header (troca de contexto PDV/retaguarda)

### Branding white-label
- API `/api/v1/branding` — nome exibido, cor primária, URL do logo
- Seção **Marca** em `/configuracoes`
- Sidebar e CSS `--brand-primary` dinâmicos

### Produção
- `docker-compose.prod.example.yml` + `.env.prod.example`

---

## Upgrade de v1.1.0

```bash
git fetch --tags
git checkout v1.2.0
docker compose up -d --build
# Migration 022 adiciona max_lojas em plano_assinatura
```
