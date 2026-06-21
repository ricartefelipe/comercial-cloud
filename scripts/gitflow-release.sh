#!/usr/bin/env bash
# Abre PR develop → main (release GitFlow).
#
# Uso: ./scripts/gitflow-release.sh [titulo]
set -euo pipefail

TITLE="${1:-Release: develop → main}"

git fetch origin
git checkout develop
git pull origin develop

AHEAD="$(git rev-list --count origin/main..develop)"
if [[ "$AHEAD" -eq 0 ]]; then
  echo "develop não está à frente de main — nada para release." >&2
  exit 1
fi

echo "develop está $AHEAD commit(s) à frente de main."

if git log origin/main..develop --format='%B' | grep -iE \
  '(Co-authored-by:.*Cursor|Made-with:.*Cursor|cursoragent@cursor\.com)'; then
  echo "Erro: commits com rastros de IA detectados. Limpe antes do release." >&2
  exit 1
fi

git push origin develop

gh pr create --base main --head develop --title "$TITLE" --body "$(cat <<EOF
## Release GitFlow

Merge de \`develop\` em \`main\` para produção / piloto AWS.

## Checklist
- [ ] CI verde em develop
- [ ] Sem trailers de IA nos commits
- [ ] Deploy piloto após merge: \`SCHEME=https ./scripts/aws-deploy-comercial-ec2.sh\`

EOF
)"
