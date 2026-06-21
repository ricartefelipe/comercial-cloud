# GitFlow — ComercialCloud

Fluxo oficial do repositório. **Features nunca vão direto para `main`.**

## Branches

| Branch | Base | Merge em | Uso |
|--------|------|----------|-----|
| `feature/*` | `develop` | `develop` | Funcionalidades |
| `fix/*` | `develop` | `develop` | Correções não urgentes |
| `release/*` | `develop` | `main` + sync `develop` | Preparar versão |
| `hotfix/*` | `main` | `main` + sync `develop` | Correção urgente em produção |
| `develop` | — | — | Integração contínua |
| `main` | — | — | Produção / deploy piloto |

## Fluxo diário (feature)

```bash
git fetch origin
git checkout develop && git pull origin develop
git checkout -b feature/minha-tarefa

# ... commits ...

git push -u origin feature/minha-tarefa
gh pr create --base develop --title "feat: ..."
```

Após merge em `develop`, apague a branch remota se não for mais necessária.

## Release (develop → main)

Quando `develop` estiver pronta para produção:

```bash
git checkout develop && git pull origin develop
./scripts/gitflow-release.sh
```

Isso abre PR **`develop` → `main`**. Após merge:

1. CI roda em `main`
2. Workflow `sync-develop-from-main` alinha `develop` (se necessário)
3. Deploy piloto: `SCHEME=https ./scripts/aws-deploy-comercial-ec2.sh`
4. Tag semver (opcional): `git tag v1.4.0 && git push origin v1.4.0`

## Hotfix (urgente em produção)

```bash
git fetch origin
git checkout main && git pull origin main
git checkout -b hotfix/descricao-curta

# ... fix + commits ...

git push -u origin hotfix/descricao-curta
gh pr create --base main --title "fix: ..."
```

Após merge em `main`, o workflow sincroniza `develop` automaticamente.

## O que o CI bloqueia

- PR de `feature/*` ou `fix/*` **direto para `main`** (workflow `enforce-gitflow`)
- Trailers de IA nos commits (`Co-authored-by: Cursor`, `Made-with: Cursor`, etc.)

## Deploy

| Ambiente | Branch | Comando |
|----------|--------|---------|
| Piloto AWS | `main` | `./scripts/aws-deploy-comercial-ec2.sh` |
| Homolog local | `develop` | `docker compose -f docker-compose.homolog.yml up` |

## Histórico (correção jun/2026)

PRs #26–#30 foram mergeados direto em `main` por exceção operacional. `develop` foi realinhada em `a7ed33d`. O workflow antigo `sync-main-with-develop` (que **resetava main** para develop) foi **removido** e substituído por `sync-develop-from-main`.
