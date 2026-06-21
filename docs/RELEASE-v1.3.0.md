# Release v1.3.0 — ComercialCloud

**Data:** 2026-06-20

Relatórios exportáveis, convites de usuário por e-mail e melhorias operacionais.

---

## Novidades

### Relatórios
- API `/api/v1/relatorios` — resumo, vendas e estoque (JSON ou CSV)
- UI `/relatorios` — filtros de período e download `vendas.csv` / `estoque.csv`

### Convites de usuário
- Migration `023-convite-usuario`
- Admin: `POST/GET /api/v1/usuarios/convites`
- Público: `GET/POST /api/v1/public/convites/{token}`
- E-mail transacional com link `{APP_URL}/convite/{token}` (quando SMTP ativo)
- UI: botão **Convidar por e-mail** em `/usuarios` + página `/convite/[token]`

### Outros
- Seed automático de estoque (qty 0) ao criar nova loja
- Botões e menu ativo usam `--brand-primary` do branding
- `docs/PENDENCIAS-OPERADOR.md` — checklist do que só o operador configura

---

## Upgrade de v1.2.0

```bash
git fetch --tags
git checkout v1.3.0
docker compose up -d --build
# Migration 023 cria tabela convite_usuario
```

Configure opcionalmente:

```properties
comercial-cloud.convite.validade-dias=7
comercial-cloud.mail.app-url=https://app.seudominio.com.br
```
