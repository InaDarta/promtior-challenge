#!/usr/bin/env bash
# Verifica que un PR ya creado cumple las tres convenciones del repo:
# base=develop, tiene assignee, y quedo vinculado en la seccion
# "Development" del issue que cierra (closingIssuesReferences).
#
# Uso: verify_pr.sh <numero-de-pr>
# Imprime un resumen legible y termina con status != 0 si algo falta,
# para que quien invoque la skill sepa que hay que corregir algo a mano.
set -euo pipefail

pr="${1:?Uso: verify_pr.sh <numero-de-pr>}"

# gh trae su propio motor jq via --jq: no depende de tener el binario jq instalado.
url=$(gh pr view "$pr" --json url --jq '.url')
base=$(gh pr view "$pr" --json baseRefName --jq '.baseRefName')
assignee_logins=$(gh pr view "$pr" --json assignees --jq '[.assignees[].login] | join(", ")')
linked_issues=$(gh pr view "$pr" --json closingIssuesReferences --jq '[.closingIssuesReferences[].number] | join(", ")')

status=0

echo "PR: $url"

if [[ "$base" == "develop" ]]; then
  echo "OK  base=develop"
else
  echo "MAL base=$base (deberia ser develop)"
  status=1
fi

if [[ -n "$assignee_logins" ]]; then
  echo "OK  assignee(s): $assignee_logins"
else
  echo "MAL sin assignee"
  status=1
fi

if [[ -n "$linked_issues" ]]; then
  echo "OK  Development vinculado a issue(s): $linked_issues"
else
  echo "MAL sin issue vinculado en Development (falta \"Closes #N\" en el body, o el issue referenciado no existe)"
  status=1
fi

exit "$status"
