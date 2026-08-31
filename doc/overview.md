# Overview

## Cómo se encaró el problema
- El enunciado deja lenguaje y framework libres. Elegí Java 25 + Spring Boot + LangChain4j en vez
  del ecosistema Python donde nació LangChain, para que una reserva inválida sea **irrepresentable**
  en el tipo (`record` + `sealed interface`), no solo rechazada en runtime ([ADR 0001](adr/0001-stack.md)).
- Arquitectura hexagonal estricta bajo `com.promtior.booking`: `domain` (Java puro, sin Spring ni
  JPA, se prueba sin levantar contexto), `application` (casos de uso y puertos, depende de `domain`
  y nunca al revés), `infrastructure` (único lugar con configuración de framework: REST,
  persistencia, seguridad JWT, capa de LLM).
- Gemini como proveedor primario por su tool calling, con Groq de respaldo automático — el proveedor
  que el propio enunciado sugiere — y Ollama para desarrollo offline, los tres detrás del mismo
  `ChatModel` ([ADR 0013](adr/0013-proveedor-de-modelo.md), [ADR 0009](adr/0009-limites-del-tier-gratuito-de-gemini.md)).
- Trabajé con issue-driven development desde el día uno: diez épicas y 43 sub-issues del tamaño de
  un PR cada uno, con objetivo, alcance y criterio de aceptación escritos antes de la primera línea
  de código ([`doc/epics/`](epics/)). El recorrido completo de una pregunta a una respuesta está
  dibujado en el [diagrama de componentes](diagrams/component-diagram.png).

## Metodología (breve)
- Camino crítico marcado desde el arranque (`E00→E01→E02→E03→E04→E05→E08→E09`), con `E06`
  (interfaz) y `E07` (testing/evaluación) señaladas de antemano como recortables si el tiempo
  apretaba — al final no hizo falta cortar nada de eso.
- Desarrollo generativo: delego cada sub-issue en Claude Code, que trabaja en un worktree de git
  aislado por rama siguiendo las reglas fijadas en `CLAUDE.md` (arquitectura hexagonal, Spotless
  obligatorio, un ADR por decisión con su alternativa descartada, PR siempre contra `develop`). Yo
  priorizo qué sigue, reviso cada PR antes de mergearlo y tomo las decisiones de producto que el
  enunciado deja abiertas.

## Lógica de implementación: el dominio decide, el modelo orquesta
- Las reglas de negocio (RN-01 a RN-08) viven como invariantes de tipos en `domain` —
  constructores compactos de `record` y un `BookingError` sellado y exhaustivo — nunca como
  validación confiada al modelo ([ADR 0015](adr/0015-reglas-en-el-dominio-no-en-el-prompt.md)).
- El system prompt describe esas reglas en lenguaje llano solo para que el modelo las explique, con
  la instrucción explícita de que **no las aplica por su cuenta**: confirma con el resultado real de
  la tool, no con lo que "le parece razonable" a partir del pedido.
- Las `@Tool` son adaptadores finos sin lógica propia sobre los casos de uso de `application`;
  `BookingError` expone `code()`/`message()` en un `switch` exhaustivo que consumen por igual el
  contrato REST `problem+json` y el resultado que reciben las tools, para que REST y chat nunca
  puedan divergir sobre qué significa un mismo error.
- La identidad nunca es un parámetro que el modelo pueda rellenar: `CurrentUserProvider` la resuelve
  siempre desde el JWT vía `SecurityContext`, nunca del texto del chat ni de un argumento de tool —
  hace la suplantación estructuralmente imposible, no solo improbable
  ([ADR 0007](adr/0007-resolucion-del-usuario-actual.md)).

## Desafíos encontrados y cómo se resolvieron
- Un mismo síntoma en producción escondía tres bugs apilados: un `ClassCastException` de
  LangChain4j 1.0.0 al combinar parámetros con tools (arreglado actualizando a 1.2.0), el mismo
  antipatrón replicado en mi propio wrapper de `ChatModel` diferido (`doChat()` en vez de `chat()`),
  y un `IllegalStateException` de usuario no autenticado en el hilo de streaming. Los tres exigieron
  reproducir el camino real contra el proveedor — invisibles para los tests con stub.
- La doble reserva bajo carga concurrente necesitó una segunda línea de defensa en la base:
  `EXCLUDE USING gist` de Postgres, con el hallazgo de que bajo inserts concurrentes el conflicto a
  veces se resuelve como *deadlock* (`40P01`) y no como `exclusion_violation` (`23P01`) — hubo que
  cubrir los dos SQLSTATE ([ADR 0005](adr/0005-constraint-de-exclusion.md)).
- El tier gratuito de Gemini resultó mucho más chico de lo esperado (20 requests/día, y los `503`
  fallidos igual consumen cupo), lo que obligó a un failover automático a Groq con reintentos
  acotados, más rate limit y health check ya en producción para no agotar la cuota durante la
  evaluación ([ADR 0009](adr/0009-limites-del-tier-gratuito-de-gemini.md),
  [ADR 0012](adr/0012-health-check-y-rate-limit-en-el-chat.md)).
- Reutilizar `BookingRange` (pensado para el máximo de 3 horas de una reserva) para representar el
  rango de una *consulta* de disponibilidad rompía pedir la agenda de un día completo
  (`400 MAX_DURATION_EXCEEDED`); lo resolví separando el concepto en `QueryRange`, con las mismas
  reglas de horario de oficina pero sin el límite de duración.
- El catálogo de modelos gratuitos cambia en semanas, no en meses: `gemini-2.5-flash` y los modelos
  default de Groq quedaron discontinuados durante el desarrollo — la lección fue que fijar un nombre
  de modelo en configuración es una decisión con fecha de vencimiento conocida, no un dato estático.

## Huecos del enunciado y qué se decidió
- Capacidad de sala sin números concretos → escalera fija A=4, B=6, C=8, D=12, E=20 en un enum
  cerrado, decisión de producto propia sin UI de administración porque el enunciado no pide gestión
  de salas, solo reservarlas ([ADR 0014](adr/0014-capacidades-de-las-salas.md)).
- Horario de oficina y reservas en el pasado sin definir → lunes a viernes 08:00–20:00 en
  `America/Montevideo` como invariante estructural, con `Clock` inyectable para el rechazo de
  reservas pasadas ([ADR 0003](adr/0003-horario-de-oficina-y-clock.md)).
- Proveedor de modelo y criterio de fallback sin definir → Gemini primario, Groq de respaldo
  automático, Ollama para desarrollo offline, los tres detrás del mismo perfil de Spring
  ([ADR 0001](adr/0001-stack.md), [ADR 0013](adr/0013-proveedor-de-modelo.md)).
- El enunciado da por sentado un notebook en Python y el proyecto está en Java → notebook con
  `rapaio-jupyter-kernel`, verificado de punta a punta contra JDK 25 antes de comprometerme
  ([ADR 0002](adr/0002-notebook-java-o-python.md)).
- No se especifica si cancelar una reserva ajena debe distinguirse de cancelar una que no existe →
  mismo error (403) en los dos casos, para no regalar una forma de enumerar reservas ajenas probando
  ids al azar ([ADR 0008](adr/0008-cancelacion-de-reservas-y-exposicion-del-id.md)).
