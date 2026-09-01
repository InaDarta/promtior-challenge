# Overview

## 1. Cómo se encaró el problema

- Antes de escribir código, relevé los requisitos reales del challenge: la lógica de negocio de las
  reglas de reserva (RN-01 a RN-08), hoy la fuente de verdad de `domain`.
- Elegí el stack en base a experiencia real, no a una opción genérica. **Java 25** para el backend,
  por ser el lenguaje con el que trabajo habitualmente: me daba la confianza necesaria para levantar
  un backend sólido, con capas bien separadas (endpoint, controller, repositorio, lógica de negocio)
  y el principio de responsabilidad única, en lugar de adoptar un lenguaje nuevo bajo la presión de
  un plazo de siete días.
- **PostgreSQL**, porque ya lo había utilizado en prácticas de la facultad y cubría exactamente lo
  necesario, en particular el constraint de exclusión contra el solapamiento de reservas
  ([ADR 0005](adr/0005-constraint-de-exclusion.md)).
- **React** para el frontend, por ser la tecnología con la que trabaja Promtior y por su cercanía
  con Vue (mi experiencia previa), lo que me permitió resolver el frontend acotado que pedía el
  challenge sin invertir tiempo adicional en aprender un framework nuevo desde cero.
- **Vite** como herramienta de build, scaffoldeado con `npm create vite@latest`
  ([frontend/README.md](../frontend/README.md)). El frontend es una SPA sin necesidad de
  server-side rendering (dos pantallas, login y chat, detrás de autenticación), así que un
  framework full-stack como Next.js hubiera sumado complejidad sin beneficio. Frente a
  Create React App (ya deprecado por el propio equipo de React), Vite da arranque y HMR
  casi instantáneos gracias a módulos ES nativos en desarrollo, con una configuración mínima
  ([frontend/vite.config.js](../frontend/vite.config.js)) que no le restó tiempo al resto del
  challenge.
- **Gemini** como proveedor del agente conversacional, porque ya lo había integrado antes en un
  proyecto de la facultad. Esa experiencia previa redujo el riesgo en la parte más nueva del
  challenge: el tool calling con un modelo de lenguaje real.
- Una vez definido el stack, adopté una arquitectura hexagonal estricta bajo `com.promtior.booking`:
  `domain` (Java puro, sin Spring ni JPA, se prueba sin levantar contexto), `application` (casos de
  uso y puertos, depende de `domain` y nunca al revés) e `infrastructure` (único lugar con
  configuración de framework: REST, persistencia, seguridad JWT y capa de LLM). El recorrido
  completo de una pregunta hasta su respuesta está documentado en el
  [diagrama de componentes](diagrams/component-diagram.png).

## 2. Metodología de trabajo

- Con los requisitos y el stack definidos, armé un plan dividido en diez épicas para sostener el
  mejor ritmo de trabajo posible durante los siete días de desarrollo ([`doc/epics/`](epics/)).
- Dividí cada épica en 43 sub-issues con requerimientos acotados, del tamaño de un pull request cada
  uno. Esto me permitió correr sesiones de trabajo en paralelo, cada una limitada a una sola tarea,
  manteniendo un contexto ajustado y favoreciendo el mejor resultado posible por sesión, en lugar de
  una única sesión intentando abarcar todo el alcance a la vez.
- Esa misma división me permitió paralelizar el trabajo real, no solo la planificación: backend por
  un lado, frontend por otro e integración con el agente de IA externo (Gemini) por otro.
- Construí y refiné el contexto del proyecto de forma incremental en `CLAUDE.md` (arquitectura,
  convenciones y reglas de trabajo), de modo que cada nueva sesión de Claude Code partiera del mismo
  criterio que las anteriores, en lugar de tener que redescubrirlo. Priorizo qué sub-issue sigue,
  reviso cada pull request antes de integrarlo y tomo las decisiones de producto que el enunciado
  deja abiertas.

## 3. Lógica de implementación

- Implementé las reglas de negocio (RN-01 a RN-08) como invariantes de tipos en `domain`:
  constructores compactos de `record` y una interfaz `BookingError` sellada y exhaustiva, nunca como
  validación confiada al modelo ([ADR 0015](adr/0015-reglas-en-el-dominio-no-en-el-prompt.md)).
- El system prompt describe esas reglas en lenguaje natural únicamente para que el modelo pueda
  explicarlas, con la instrucción explícita de que **no las aplica por su cuenta**: debe confirmar
  con el resultado real de la tool, no con lo que resulte razonable a partir del pedido.
- Diseñé las `@Tool` como adaptadores livianos, sin lógica propia, sobre los casos de uso de
  `application`. `BookingError` expone `code()` y `message()` mediante un `switch` exhaustivo, que
  consumen por igual el contrato REST `problem+json` y el resultado que reciben las tools, de modo
  que REST y chat nunca puedan divergir sobre el significado de un mismo error.
- Decidí que la identidad del usuario nunca sea un parámetro que el modelo pueda completar:
  `CurrentUserProvider` la resuelve siempre a partir del JWT mediante `SecurityContext`, nunca del
  texto del chat ni de un argumento de tool; esto hace que la suplantación de identidad sea
  estructuralmente imposible, no solo improbable
  ([ADR 0007](adr/0007-resolucion-del-usuario-actual.md)).

## 4. Desafíos encontrados y cómo se resolvieron

- **El deploy fue uno de los desafíos más importantes.** En entorno local, con Docker, no tuve
  inconvenientes; en Railway, en cambio, tuve que resolver requisitos adicionales, particularmente
  la configuración de las variables de entorno, apoyándome en la documentación de la plataforma.
  Atravesé una curva de aprendizaje real, pero una vez encaminado el proceso confirmé el valor de
  Railway: una interfaz clara, un buen manejo de logs y un despliegue práctico desde GitHub, sin
  necesidad de configurar infraestructura adicional
  ([runbook de despliegue](deployment-runbook.md)).
- **El límite de solicitudes de Gemini fue el problema más importante del lado de la IA.** Por este
  motivo, establecí Groq como segundo proveedor por defecto desde el inicio del desarrollo, y no
  únicamente como red de contención en producción: contar con dos proveedores (y dos cuentas de
  prueba, una por proveedor) me permitió seguir testeando en paralelo sin agotar la cuota durante el
  desarrollo ([ADR 0009](adr/0009-limites-del-tier-gratuito-de-gemini.md),
  [ADR 0013](adr/0013-proveedor-de-modelo.md)).
- **La integración con LangChain4j me presentó un problema recurrente una vez desplegada la
  aplicación:** las respuestas no se parseaban correctamente y se producían errores apenas el
  asistente respondía utilizando una tool. Al investigar en profundidad (issue #106) identifiqué un
  desalineamiento de versiones en LangChain4j 1.0.0: el `ChatModel` perdía el tipo concreto de los
  parámetros al combinarlos con los de la tool, y el cast posterior producía un `ClassCastException`.
  Actualizar las cinco dependencias a la versión 1.2.0 resolvió el problema.
- **La doble reserva bajo carga concurrente** me obligó a agregar una segunda línea de defensa a
  nivel de base de datos: `EXCLUDE USING gist` de PostgreSQL. Durante las pruebas identifiqué que,
  bajo inserts concurrentes, el conflicto en ocasiones se resuelve como *deadlock* (`40P01`) y no
  como `exclusion_violation` (`23P01`); por lo tanto, tuve que cubrir ambos códigos SQLSTATE
  ([ADR 0005](adr/0005-constraint-de-exclusion.md)).
- **La lógica de negocio de la API en sí no me dio mayores dificultades**, al tratarse de una API de
  alcance acotado. El desarrollo fue ágil y solo requirió ajustes menores sobre la marcha, como
  separar `QueryRange` de `BookingRange` una vez que detecté que reutilizar el value object de
  reserva para representar una consulta de disponibilidad impedía solicitar la agenda de un día
  completo.
- Incorporé Swagger/OpenAPI sobre la API REST para tener un entorno de prueba simple y accesible, en
  el que cualquier persona pueda probar los endpoints sin pasar por el chat.

## 5. Huecos del enunciado y qué se decidió

| Vacío del enunciado | Decisión adoptada | Referencia |
|---|---|---|
| No se especifican valores concretos de capacidad por sala. | Escalera fija A=4, B=6, C=8, D=12, E=20 en un enum cerrado; decisión de producto propia, sin interfaz de administración, dado que el enunciado no requiere gestión de salas sino únicamente su reserva. | [ADR 0014](adr/0014-capacidades-de-las-salas.md) |
| No se define el horario de oficina ni el tratamiento de reservas en el pasado. | Lunes a viernes de 08:00 a 20:00 en `America/Montevideo`, como invariante estructural, con `Clock` inyectable para el rechazo de reservas pasadas. | [ADR 0003](adr/0003-horario-de-oficina-y-clock.md) |
| No se define el proveedor de modelo ni el criterio de failover. | Gemini como proveedor primario, Groq como respaldo automático y Ollama para desarrollo offline, los tres detrás del mismo perfil de Spring. | [ADR 0001](adr/0001-stack.md), [ADR 0013](adr/0013-proveedor-de-modelo.md) |
| El enunciado presupone un notebook en Python, mientras el proyecto está desarrollado en Java. | Notebook implementado con `rapaio-jupyter-kernel`, verificado de punta a punta contra JDK 25 antes de adoptar la decisión. | [ADR 0002](adr/0002-notebook-java-o-python.md) |
| No se especifica si cancelar una reserva ajena debe distinguirse de cancelar una reserva inexistente. | Se devuelve el mismo error (403) en ambos casos, para no habilitar la enumeración de reservas ajenas mediante identificadores al azar. | [ADR 0008](adr/0008-cancelacion-de-reservas-y-exposicion-del-id.md) |

## 6. Mejoras a futuro

Lo siguiente son decisiones de alcance, no errores: dado el plazo de siete días del challenge,
prioricé lo que pide el enunciado y descarté explícitamente lo demás (ver los ADRs referenciados
en cada punto). Quedan documentadas acá como el trabajo que haría a continuación si este proyecto
siguiera en desarrollo.

- **Catálogo de salas administrable.** `Room` es hoy un enum cerrado con capacidad fija (A=4, B=6,
  C=8, D=12, E=20), sembrado por Flyway y sin UI de administración
  ([ADR 0014](adr/0014-capacidades-de-las-salas.md)). Agregar o modificar una sala requiere una
  migración y un release. Con más alcance, sumaría un catálogo persistido y editable en runtime, y
  un rol `admin` propio — hoy `AppUserDetailsService` no distingue roles, todos los usuarios
  sembrados tienen los mismos permisos.
- **Rate limiting distribuido.** `ChatRateLimiter` guarda los cupos de bucket4j en un
  `ConcurrentHashMap` del proceso
  ([infrastructure/ratelimit](../src/main/java/com/promtior/booking/infrastructure/ratelimit)),
  limitación que su propio `package-info.java` deja explícita: sobrevive un único proceso, no un
  restart ni varias instancias. Escalar a más de una réplica en Railway (o cualquier balanceo
  horizontal) requeriría mover el cupo a un backend compartido, típicamente Redis.
- **Suite de evaluación del agente en CI.** Hoy `BookingAgentEvalRunner` corre a mano y cubrió 8 de
  20 casos del dataset en la última corrida, limitado por el tier gratuito de Gemini (RPD=20) y de
  Groq (8000 TPM) — ver [doc/eval/E07.3-resultados.md](eval/E07.3-resultados.md). Con presupuesto
  de API dedicado, correría el dataset completo en un job de CI aparte, no bloqueante en cada PR,
  para tener señal continua de regresión de comportamiento del agente y no solo de compilación.
- **`GROQ_MODEL_NAME` desactualizado.** Esa misma corrida detectó que los modelos por default de
  `application.yml` (`llama-3.3-70b-versatile`, `llama-3.1-8b-instant`) ya no existen en el
  catálogo de Groq; quedó anotado como "issue aparte" sin abrir. Lo resolvería fijando un modelo
  vigente con tool calling y agregando un chequeo de arranque que falle rápido si el modelo
  configurado no está disponible, en vez de descubrirlo recién en el primer mensaje de un usuario
  real.
- **Logs estructurados y sink externo.** [ADR 0016](adr/0016-politica-de-logs.md) descartó JSON
  logging a propósito: Railway ya parsea texto plano y no hay un sink externo (Datadog, ELK, Loki)
  en el alcance de este proyecto. Con un volumen de uso real, revisaría esa decisión.
- **Tracing de Langfuse por default.** Hoy es opt-in vía `LANGFUSE_TRACING_ENABLED`
  ([ADR 0011](adr/0011-trazas-de-llm-y-tool-calls-con-langfuse.md)). En un entorno productivo real
  lo dejaría encendido por default con muestreo, en vez de depender de que alguien lo prenda a
  mano para investigar un caso puntual.
- **Cobertura de tests medida.** `./mvnw verify` corre tests y Spotless pero no mide cobertura: no
  hay JaCoCo ni un umbral mínimo en el pipeline de CI
  ([.github/workflows/ci.yml](../.github/workflows/ci.yml)). Lo agregaría con un piso alto en
  `domain`/`application` (Java puro, se presta a cobertura casi total) y más laxo en
  `infrastructure`.
- **Tests automatizados de frontend.** `frontend/package.json` solo tiene `oxlint` como
  verificación estática; no hay Vitest/Testing Library para componentes ni Playwright/Cypress para
  el flujo end-to-end (login → chat → tool call → agenda). Hoy esa verificación es manual. CI
  tampoco tiene un job de frontend: un `vite build` roto no se detecta hasta el deploy en Railway.
- **Métrica de failover de proveedor.** El failover automático Gemini → Groq
  ([ADR 0009](adr/0009-limites-del-tier-gratuito-de-gemini.md),
  [ADR 0013](adr/0013-proveedor-de-modelo.md)) se loguea en WARN pero no se expone como métrica. Un
  contador vía Actuator (o un span dedicado en Langfuse) permitiría ver en un dashboard cuánto
  tiempo el sistema está operando en modo de respaldo, sin tener que grepear logs.
