#!/usr/bin/env bash
# Cria branch feature/* a partir de develop atualizada.
#
# Uso: ./scripts/gitflow-nova-feature.sh nome-da-tarefa
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Uso: $0 nome-da-tarefa" >&2
  echo "Ex.: $0 pdv-desconto-item" >&2
  exit 1
fi

SLUG="$1"
BRANCH="feature/${SLUG}"

git fetch origin
git checkout develop
git pull origin develop
git checkout -b "$BRANCH"

echo "Branch criada: $BRANCH (a partir de develop)"
echo "Após os commits: git push -u origin HEAD && gh pr create --base develop"
