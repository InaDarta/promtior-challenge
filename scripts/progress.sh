#!/usr/bin/env bash
#
# Genera doc/PROGRESO.md a partir de los issues de GitHub.
#
# Lo corre GitHub Actions ante cada apertura, cierre o etiquetado de un issue,
# pero también sirve a mano:
#
#   ./scripts/progress.sh
#
# Única dependencia: gh. Usa su jq embebido, así que no hace falta instalar jq.
#
set -euo pipefail

REPO="${GITHUB_REPOSITORY:-InaDarta/promtior-challenge}"
PROJECT_URL="${PROJECT_URL:-https://github.com/users/InaDarta/projects/3}"
OUT="doc/PROGRESO.md"

cd "$(dirname "$0")/.."
mkdir -p doc

NOW="$(date -u +'%Y-%m-%d %H:%M UTC')"

gh issue list --repo "$REPO" --limit 200 --state all \
  --json number,title,state,labels,milestone --jq '

def names: [.labels[].name];
def idpart: (.title | split("] ")[0] | ltrimstr("["));
def nm:     (.title | split("] ") | .[1:] | join("] "));
def isEpic: (idpart | contains(".") | not);
def eid:    (idpart | split(".")[0]);
def prio:   (names | map(select(startswith("prio:"))) | first // "prio:P1")[5:];
def area:   (names | map(select(startswith("area:"))) | first // "area:otro")[5:];
def ms:     (.milestone.title // "-");

def st:
  if   .state == "CLOSED"              then (if (names | index("status:descoped")) then "descoped" else "done" end)
  elif (names | index("status:descoped")) then "descoped"
  elif (names | index("status:wip"))      then "wip"
  else "todo" end;

def icon: {done:"✅", wip:"🟡", todo:"⬜", descoped:"⬛"}[st];

def bar($d; $t):
  (if $t <= 0 then 0 else (($d / $t * 24) | floor) end) as $n
  | (if $n > 0 then ("█" * $n) else "" end)
  + (if 24 - $n > 0 then ("░" * (24 - $n)) else "" end);

def pct($d; $t): if $t <= 0 then 0 else (($d / $t * 100) | round) end;

. as $all
| ($all | map(select(isEpic))        | sort_by(.title)) as $epics
| ($all | map(select(isEpic | not))  | sort_by(.title)) as $subs
| ($subs | map(select(st != "descoped")))               as $live
| ($live | map(select(st == "done")) | length)          as $done
| ($live | length)                                      as $total
| ["E00","E01","E02","E03","E04","E05","E08","E09"]     as $crit

| "# Progreso",
  "",
  "> Generado automáticamente por [`scripts/progress.sh`](../scripts/progress.sh) el __NOW__. No editar a mano.",
  "",
  "`\(bar($done; $total))` **\(pct($done; $total))%** — \($done) de \($total) sub-issues cerrados",
  "",
  "| Métrica | |",
  "|---|---|",
  "| Épicas | \($epics | length) |",
  "| Sub-issues | \($total) |",
  "| En curso | \($live | map(select(st == "wip")) | length) |",
  "| Restantes | \($total - $done) |",
  "| P0 pendientes | \($live | map(select(st != "done" and prio == "P0")) | length) |",
  "| Camino crítico pendiente | \($live | map(select(st != "done" and (eid | IN($crit[])))) | length) |",
  "",
  "## Milestones",
  "",
  "| Milestone | Progreso | Cerrados | |",
  "|---|---|---|---|",
  ( ["M1 Core", "M2 Agente", "M3 Entrega"][] as $m
    | ($live | map(select(ms == $m)))            as $g
    | ($g | map(select(st == "done")) | length)  as $d
    | "| **\($m)** | `\(bar($d; ($g | length)))` | \($d)/\($g | length) | \(pct($d; ($g | length)))% |"
  ),
  "",
  "## Épicas",
  "",
  "| | Épica | Sub-issues | Cerrados | |",
  "|---|---|---|---|---|",
  ( $epics[]
    | . as $e
    | ($subs | map(select(eid == ($e | idpart))))  as $g
    | ($g | map(select(st != "descoped")))         as $gl
    | ($gl | map(select(st == "done")) | length)   as $d
    | "| \(if ($e | idpart | IN($crit[])) then "🔴" else "⚪" end) "
      + "| [\($e | idpart)](https://github.com/__REPO__/issues/\($e.number)) \($e | nm) "
      + "| \($g | map(icon) | join(" ")) "
      + "| \($d)/\($gl | length) | \(pct($d; ($gl | length)))% |"
  ),
  "",
  "🔴 camino crítico · ⚪ recortable · ✅ cerrado · 🟡 en curso · ⬜ pendiente · ⬛ descartado",
  "",
  "## Por área",
  "",
  "| Área | Progreso | Cerrados |",
  "|---|---|---|",
  ( ($live | group_by(area) | sort_by(-length))[] as $g
    | ($g | map(select(st == "done")) | length)   as $d
    | "| `\($g[0] | area)` | `\(bar($d; ($g | length)))` | \($d)/\($g | length) |"
  ),
  "",
  "## Lo próximo",
  "",
  "Pendientes del camino crítico, en orden de dependencia:",
  "",
  ( [ $subs[] | select(st == "todo") | select(eid | IN($crit[])) ]
    | sort_by(.title) | .[0:6][]
    | "- [ ] [#\(.number)](https://github.com/__REPO__/issues/\(.number)) `\(idpart)` \(nm) · **\(prio)**"
  ),
  "",
  "---",
  "",
  "Tablero interactivo: [__PROJECT__](__PROJECT__)",
  "",
  "Para marcar una tarea **en curso**, agregale la label `status:wip` al issue cuando abras su rama.",
  "Para descartarla del alcance, `status:descoped` y cerrala con el motivo escrito.",
  ""
' | sed -e "s|__REPO__|$REPO|g" \
      -e "s|__NOW__|$NOW|g" \
      -e "s|__PROJECT__|$PROJECT_URL|g" > "$OUT"

echo "generado $OUT"
