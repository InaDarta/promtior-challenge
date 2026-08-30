# Architecture Decision Records

Registro de decisiones de arquitectura significativas: se escriben cuando se toma la decisión,
no como reconstrucción al final. Formato liviano tipo [MADR](https://adr.github.io/madr/):
contexto, decisión, alternativas descartadas y consecuencias. Una decisión sin alternativa
descartada casi seguro no ameritaba un ADR.

Nueva ADR: copiar [`template.md`](template.md) a `NNNN-slug.md` con el siguiente número
correlativo.

## Índice
- [0001](0001-stack.md) — Stack: Java 25, Spring Boot y LangChain4j
- [0002](0002-notebook-java-o-python.md) — Notebook del demo: Java con rapaio-jupyter-kernel
- [0003](0003-horario-de-oficina-y-clock.md) — Horario de oficina, rechazo de reservas pasadas y `Clock` inyectable
- [0004](0004-persistencia-de-booking.md) — Persistencia de `Booking`: sin id de dominio, columnas planas para las FK
- [0005](0005-constraint-de-exclusion.md) — Constraint de exclusión contra la doble reserva y su traducción a `BookingError`
- [0006](0006-spring-security-y-jwt.md) — Spring Security con sesiones sin estado y JWT vía jjwt
- [0007](0007-resolucion-del-usuario-actual.md) — Resolución del usuario actual vía un puerto de `application`
- [0008](0008-cancelacion-de-reservas-y-exposicion-del-id.md) — Cancelación de reservas: id expuesto solo por el puerto, 403 que no revela existencia
- [0009](0009-limites-del-tier-gratuito-de-gemini.md) — Límites del tier gratuito de Gemini y confirmación del proveedor primario
- [0010](0010-streaming-del-chat.md) — Streaming del chat: errores in-band, `fetch` en vez de `EventSource`, failover acotado
- [0011](0011-trazas-de-llm-y-tool-calls-con-langfuse.md) — Trazas de LLM y tool calls con Langfuse vía OpenTelemetry
