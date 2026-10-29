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
2. Workflow `check-develop-sync` relata se `develop` ficou atrás (não altera branches)
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

Após merge em `main`, o workflow `check-develop-sync` apenas **relata** se `develop` ficou atrás.
A sincronização é manual, por PR de `main` para `develop`: nenhuma automação promove, mescla ou
reescreve branches.

## O que o CI bloqueia

- PR de `feature/*` ou `fix/*` **direto para `main`** (workflow `enforce-gitflow`)
- Trailers de co-autoria de ferramentas de IA nos commits

## Deploy

| Ambiente | Branch | Comando |
|----------|--------|---------|
| Piloto AWS | `main` | `./scripts/aws-deploy-comercial-ec2.sh` |
| Homolog local | `develop` | `docker compose -f docker-compose.homolog.yml up` |

## Histórico (correção jun/2026)

PRs #26–#30 foram mergeados direto em `main` por exceção operacional. `develop` foi realinhada em `a7ed33d`. O workflow antigo `sync-main-with-develop` (que **resetava main** para develop) foi **removido**.

## Histórico (correção out/2026)

O repositório passou a operar somente com `main`: a branch `develop` **não existe** no remoto. O
workflow `sync-develop-from-main` fazia `git fetch origin main develop` e falhava em todo push para
`main` (`fatal: couldn't find remote ref develop`); quando não falhava, criava a branch e publicava
com `git reset --hard`. Ele foi substituído por `check-develop-sync`, que é **somente leitura**:
compara as refs publicadas e informa o resultado (branch ausente, em sincronia ou divergente) sem
`reset`, `push`, criação de branch, abertura de PR ou auto-merge. A lógica fica em
`scripts/check-develop-sync.sh`, executável localmente.

As tags `v1.0.0`–`v1.3.0` ainda carregam o workflow `sync-main-with-develop`, que fazia
`git reset --hard origin/develop` em `main` seguido de `git push origin main --force-with-lease`.
Ele não está mais em `main` e não dispara por push, mas um `workflow_dispatch` apontado para uma
dessas tags ainda o executaria. Remover ou reescrever tags publicadas é decisão do proprietário.
