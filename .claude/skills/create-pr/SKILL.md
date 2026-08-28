---
name: create-pr
description: Abre un Pull Request en el repo promtior-challenge siguiendo sus convenciones — usa .github/pull_request_template.md, apunta siempre a develop (nunca a main), asigna el PR al mismo usuario que tiene asignado el issue que cierra, y confirma que el issue quede vinculado en la seccion "Development" via "Closes #N". Usar esta skill cada vez que se pide crear, abrir o mandar un PR/pull request de este repo, con o sin numero de issue explicito (ej. "/create-pr", "/create-pr 23", "abrime el PR del issue 23", "creame el pull request de esta rama").
---

# Crear un PR en promtior-challenge

Esta skill automatiza abrir un PR respetando las convenciones del repo, documentadas en
[CLAUDE.md](../../../CLAUDE.md) y [doc/epics/README.md](../../../doc/epics/README.md): PR contra
`develop`, template completo (sin placeholders), assignee igual al del issue, y el issue linkeado
en Development.

Crear un PR es una accion visible para terceros (lo ve cualquiera con acceso al repo) e
irreversible en el sentido de que no se puede "deshacer" limpiamente — por eso el paso 5 pide
confirmacion explicita antes de ejecutar `gh pr create`, siguiendo las reglas de seguridad de la
sesion. No te saltees ese paso aunque el usuario ya haya pedido "creame el PR" — mostrale primero
que vas a crear.

## 1. Resolver el numero de issue

- Si el usuario lo paso como argumento, usar ese.
- Si no, correr `bash .claude/skills/create-pr/scripts/detect_issue.sh` — infiere el numero desde
  el nombre de la rama actual (las ramas de este repo se llaman `<numero>-<slug>`, generadas por
  GitHub al crear la rama desde el issue; **no** siguen el patron `feature/E0X.N-slug` que
  describe el issue, pese a lo que dice el CLAUDE.md sobre nombres de rama).
- Si el script falla (rama sin numero, ej. estas en `develop`/`main`, o una rama `claude/...`
  generica), preguntale al usuario el numero de issue en vez de adivinar.

## 2. Chequear que no exista ya un PR para esta rama

```bash
gh pr view --json number,url 2>/dev/null
```

Si ya existe, avisale al usuario y pregunta si quiere editarlo en vez de crear uno nuevo (esta
skill no cubre editar PRs existentes).

## 3. Reunir el contexto del issue y del diff

```bash
gh issue view <numero> --json title,assignees,body
git diff develop...HEAD --stat
git log develop..HEAD --oneline
```

- **Titulo del PR**: en este repo los issues de sub-tarea ya tienen el titulo con el formato
  correcto (`[E0X.N] Descripcion en imperativo`, ej. `[E02.1] Esquema Flyway y entidades JPA`) —
  usar el titulo del issue tal cual, tal como lo confirman los PRs `#60`-`#67`. Si el issue no
  sigue ese formato (por ejemplo un chore/spike suelto), preguntar o usar buen criterio con un
  titulo imperativo corto.
- **Assignee**: tomar `assignees[0].login` del issue. Si el issue no tiene assignee, usar
  `@me` (el usuario de `gh auth status`).

## 4. Completar el template con contenido real

Leer [.github/pull_request_template.md](../../../.github/pull_request_template.md) y llenar cada
seccion con el diff real — nunca dejar los placeholders (`<!-- ... -->`) ni el checkbox de
`./mvnw verify` sin marcar si no lo corriste. Basate en `git diff develop...HEAD` y en el propio
trabajo hecho en la sesion (no en el issue) para redactar "Qué cambia": el issue describe lo que
había que hacer, el PR describe lo que efectivamente se hizo, que puede diferir en detalles.

Reglas para cada seccion:

- **Qué cambia**: 2-4 bullets concretos, no una lista de nombres de archivos.
- **Cómo se probó**: correr `./mvnw verify` (con `JAVA_HOME` apuntando al JDK 25, ver CLAUDE.md)
  antes de armar el body, y marcar el checkbox segun el resultado real. Si algo no corrio en este
  entorno (ej. tests de Testcontainers por la restriccion de sandbox documentada en CLAUDE.md),
  decirlo explicitamente en vez de tildar el checkbox a ciegas.
- **Documentación**: nombrar el archivo de doc tocado (ADR, epics, README, package-info) o poner
  "Ninguna" si no aplica.
- **Última línea**: `Closes #<numero>` — es lo que crea el link de Development mientras el PR
  siga abierto (confirmado en PR #66; no hace falta ninguna mutation de GraphQL para un PR nuevo).

## 5. Confirmar con el usuario antes de crear el PR

Mostrar un resumen — titulo, base (`develop`), rama origen, assignee, y el body completo — y
esperar una confirmacion explicita antes de seguir. No asumas que un pedido inicial como "creame
el PR" ya cubre esta confirmacion: el usuario todavia no vio el contenido real que vas a publicar.

## 6. Crear el PR

```bash
gh pr create --base develop --title "<titulo>" --body-file <archivo-temporal> --assignee <login>
```

Usar `--body-file` (no `--body`) para evitar problemas de escapado con el body multilinea; escribir
el body a un archivo en el directorio de scratchpad de la sesion antes de este comando.

## 7. Verificar que quedo todo bien

```bash
bash .claude/skills/create-pr/scripts/verify_pr.sh <numero-de-pr>
```

Chequea las tres convenciones (base=develop, tiene assignee, tiene issue vinculado en
Development) y devuelve status != 0 si falta algo. Si el link de Development no aparecio
(el script lo marca "MAL"), revisar que el body efectivamente tenga `Closes #<numero>` con el
numero correcto — `gh pr edit <numero-de-pr> --body-file <archivo>` para corregirlo sin recrear
el PR.

## Caso borde: PR ya mergeado sin link de Development

Si en algun momento hay que arreglar esto retroactivamente en un PR que ya se mergeo (el link de
Development no persiste solo con `Closes #N` una vez mergeado), hace falta la mutation GraphQL
`addCloseIssueReferences` en vez del body — pedir los `node id` de issue y PR via
`gh api graphql` y correr la mutation. Esto es un caso raro fuera del flujo normal de crear un PR
nuevo; si aparece, tratarlo aparte en vez de intentar meterlo en este flujo.

## Reportar el resultado

Al terminar, devolver al usuario la URL del PR (de `gh pr create` o del `verify_pr.sh`) — no hace
falta repetir el body completo, ya lo vio en el paso 5.
