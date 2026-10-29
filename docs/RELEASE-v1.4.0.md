# Release v1.4.0 — ComercialCloud

**Data:** 2026-10-29

Automação de branches somente leitura.

---

## Alterações

### GitFlow
- O workflow `sync-develop-from-main` (que recriava `develop` com `reset --hard` e falhava quando ela não existia) foi substituído por `check-develop-sync`, que apenas compara `main` e `develop` e relata o estado — ausente, em sincronia ou divergente — sem escrever no remoto.
- A lógica fica em `scripts/check-develop-sync.sh`, executável localmente.
- `docs/GITFLOW.md` descreve o fluxo atual e registra que as tags `v1.0.0`–`v1.3.0` ainda carregam o workflow antigo `sync-main-with-develop`.

### Operação
- A branch `develop` passou a existir no remoto, criada a partir de `main`.
