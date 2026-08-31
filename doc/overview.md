# Overview

## Cómo se encaró el problema
- Antes de tocar código, bajé a tierra los requisitos reales del challenge: la lógica de negocio de
  las reglas de reserva (RN-01 a RN-08, hoy la fuente de verdad de `domain`).
- Elegí el stack en base a experiencia real, no a la opción "de manual". **Java 25** para el
  backend porque es el lenguaje con el que trabajo en el día a día — me daba la confianza para
  levantar un backend sólido, con capas bien separadas (endpoint, controller, repositorio, lógica de
  negocio) y *single responsibility*, en vez de arrancar de cero en un lenguaje nuevo bajo la presión
  de siete días.
- **PostgreSQL** porque ya lo usé en prácticas de la facultad y cubría exactamente lo que
  necesitaba — en particular, el constraint de exclusión contra el solapamiento de reservas
  ([ADR 0005](adr/0005-constraint-de-exclusion.md)).
- **React** para el frontend porque es la tecnología con la que trabaja Promtior, y lo bastante
  parecida a Vue (mi experiencia previa) como para animarme a resolver el acotado front que pedía el
  challenge sin perder tiempo aprendiendo un framework nuevo de cero.
- **Gemini** como proveedor del agente porque ya lo había integrado antes en un proyecto de la
  facultad — esa experiencia previa bajó el riesgo justo en la parte más nueva del challenge: tool
  calling con un LLM real.
- Arquitectura hexagonal estricta bajo `com.promtior.booking` una vez elegido el stack: `domain`
  (Java puro, sin Spring ni JPA, se prueba sin levantar contexto), `application` (casos de uso y
  puertos, depende de `domain` y nunca al revés), `infrastructure` (único lugar con configuración de
  framework: REST, persistencia, seguridad JWT, capa de LLM). El recorrido completo de una pregunta a
  una respuesta está dibujado en el [diagrama de componentes](diagrams/component-diagram.png).

## Metodología (breve)
- Con requisitos y stack bajados a tierra, armé un plan dividido en diez épicas para sostener el
  mejor ritmo de trabajo posible durante los siete días ([`doc/epics/`](epics/)).
- Cada épica se partió en 43 sub-issues con requerimientos acotados, del tamaño de un PR cada uno —
  eso permitió correr sesiones en paralelo, cada una limitada a una sola tarea, para mantener el
  contexto ajustado y sacar el mejor resultado de cada sesión en vez de una sola intentando abarcar
  todo a la vez.
- Esa misma división habilitó paralelizar el trabajo real, no solo el planeamiento: backend por un
  lado, frontend por otro, integración con el agente de IA externo (Gemini) por otro.
- Fui construyendo y afinando el contexto del proyecto en `CLAUDE.md` en cada iteración —
  arquitectura, convenciones, reglas de trabajo — así cada sesión nueva de Claude Code arrancaba con
  el mismo criterio que las anteriores en vez de tener que redescubrirlo. Yo priorizo qué sub-issue
  sigue, reviso cada PR antes de mergearlo y tomo las decisiones de producto que el enunciado deja
  abiertas.

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
- **El deploy fue uno de los desafíos más grandes.** Local con Docker no daba ningún problema, pero
  Railway exigía un par de cosas más — sobre todo cómo se seteaban las variables de entorno — que
  fui resolviendo de a una con la documentación de la plataforma. Hubo una curva de aprendizaje real,
  pero una vez encaminado encontré el valor de Railway: buena UI, buen manejo de logs, deploy
  práctico desde GitHub sin tener que configurar infraestructura aparte
  ([runbook de despliegue](deployment-runbook.md)).
- **El límite de requests de Gemini fue el problema más grande del lado de la IA.** Por eso Groq
  quedó como segundo proveedor por default, no solo como red de contención en producción: tener dos
  proveedores (y dos cuentas de prueba, una por proveedor) me permitió seguir testeando en paralelo
  sin quedarme sin cupo en pleno desarrollo ([ADR 0009](adr/0009-limites-del-tier-gratuito-de-gemini.md),
  [ADR 0013](adr/0013-proveedor-de-modelo.md)).
- **La integración con LangChain4j tuvo un problema recurrente ya con la app desplegada:** las
  respuestas no se parseaban bien y tiraba errores apenas el asistente respondía usando una tool.
  Investigando a fondo (issue #106) encontré que era un desalineamiento de versiones de LangChain4j
  1.0.0: el `ChatModel` perdía el tipo concreto de los parámetros al combinarlos con los de la tool,
  y el cast posterior explotaba con `ClassCastException`. Actualizar las cinco dependencias a la
  1.2.0 lo resolvió.
- **La doble reserva bajo carga concurrente** necesitó una segunda línea de defensa en la base:
  `EXCLUDE USING gist` de Postgres, con el hallazgo de que bajo inserts concurrentes el conflicto a
  veces se resuelve como *deadlock* (`40P01`) y no como `exclusion_violation` (`23P01`) — hubo que
  cubrir los dos SQLSTATE ([ADR 0005](adr/0005-constraint-de-exclusion.md)).
- **La lógica de negocio de la API en sí no dio mayor problema** — era una API acotada de entrada,
  así que el desarrollo fue rápido y solo necesitó ajustes chicos sobre la marcha (por ejemplo,
  separar `QueryRange` de `BookingRange` cuando reusar el value object de reserva para una consulta
  de disponibilidad rompía pedir la agenda de un día completo).
- Sumé Swagger/OpenAPI sobre la API REST para tener un ambiente de prueba simple y accesible, donde
  cualquiera puede probar los endpoints sin pasar por el chat.

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
