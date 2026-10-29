#!/usr/bin/env bash
# Verificação somente leitura do alinhamento entre main e develop (GitFlow).
#
# Não promove, não cria branch, não mescla e não escreve no remoto: apenas
# compara as refs publicadas e relata o que o operador precisa decidir.
#
# Uso: check-develop-sync.sh [remote] [base] [topic]
set -euo pipefail

remote="${1:-origin}"
base="${2:-main}"
topic="${3:-develop}"

# Anota no resumo do job quando houver um, e sempre no stdout do passo.
note() {
    local level="$1" text="$2"
    printf '%s\n' "$text"
    if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
        printf -- '- %s\n' "$text" >>"$GITHUB_STEP_SUMMARY"
    fi
    if [ -n "${GITHUB_ACTIONS:-}" ]; then
        printf '::%s::%s\n' "$level" "$text"
    fi
}

published() {
    # Lista a ref no remoto sem criar nem atualizar nada; vazio = não existe.
    git ls-remote --heads "$remote" "refs/heads/$1" 2>/dev/null | awk 'NF {print $1}'
}

base_sha="$(published "$base")"
if [ -z "$base_sha" ]; then
    note error "GitFlow: a branch base '$base' não existe em '$remote'; nada pode ser verificado."
    exit 1
fi

topic_sha="$(published "$topic")"
if [ -z "$topic_sha" ]; then
    note notice "GitFlow: '$topic' não existe em '$remote'. Nada a verificar e nada foi criado — \
o repositório trabalha somente com '$base'. Para adotar o fluxo com '$topic', crie a branch \
deliberadamente a partir de '$base'."
    exit 0
fi

# Traz as duas refs para refs de rastreamento locais; o remoto não é alterado.
git fetch --quiet --no-tags "$remote" \
    "refs/heads/$base:refs/remotes/sync-check/$base" \
    "refs/heads/$topic:refs/remotes/sync-check/$topic"

base_ref="refs/remotes/sync-check/$base"
topic_ref="refs/remotes/sync-check/$topic"

if [ "$base_sha" = "$topic_sha" ]; then
    note notice "GitFlow: '$topic' e '$base' apontam para o mesmo commit ($(git rev-parse --short "$base_ref")). Em sincronia."
    exit 0
fi

behind="$(git rev-list --count "$topic_ref..$base_ref")"
ahead="$(git rev-list --count "$base_ref..$topic_ref")"

if [ "$behind" -eq 0 ]; then
    note notice "GitFlow: '$topic' já contém todo o conteúdo de '$base' e está $ahead commit(s) à frente. \
Nada a sincronizar."
    exit 0
fi

note warning "GitFlow: '$topic' está $behind commit(s) atrás de '$base' (e $ahead à frente). \
Abra um PR de '$base' para '$topic' e resolva o merge manualmente — esta verificação não altera branches."

if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
    {
        printf '\n### Commits em `%s` ausentes em `%s`\n\n' "$base" "$topic"
        git log --no-merges --format='- `%h` %s' "$topic_ref..$base_ref" | head -20
    } >>"$GITHUB_STEP_SUMMARY"
fi
git log --no-merges --oneline "$topic_ref..$base_ref" | head -20
exit 0
