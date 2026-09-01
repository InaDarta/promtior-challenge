#!/usr/bin/env bash
# Infiere el numero de issue a partir del nombre de una rama.
# Las ramas de este repo se crean desde el issue de GitHub y quedan como
# "<numero>-<slug>" (ej. "22-e021-esquema-flyway-y-entidades-jpa"), no como
# "feature/E0X.N-slug" pese a lo que documenta el issue — GitHub genera el
# nombre solo al usar "Create a branch" desde el issue.
#
# Uso: detect_issue.sh [nombre-de-rama]
# Sin argumento, usa la rama actual. Imprime el numero por stdout;
# si no puede inferirlo, no imprime nada y sale con status 1.
set -euo pipefail

branch="${1:-$(git branch --show-current)}"

if [[ "$branch" =~ ^([0-9]+)- ]]; then
  echo "${BASH_REMATCH[1]}"
else
  exit 1
fi
