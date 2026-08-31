# 0012. Health check público y rate limit en memoria en /api/chat

## Estado
Aceptada (2026-08-30)

## Contexto
[#47](https://github.com/InaDarta/promtior-challenge/issues/47) (E08.3), previo al deploy real
en Railway (E08.2). ADR 0009 confirmó que el tier gratuito de Gemini tiene RPM=5 y RPD=20 -- unas
pocas conversaciones largas durante la evaluación, o dos evaluadores probando en paralelo, pueden
agotar la cuota del día y dejar la demo muerta justo cuando la están mirando. Hace falta además
que Railway (y cualquier monitor externo) pueda verificar que la app está viva y que la base
responde, sin necesitar un JWT para eso.

## Decisión
- **Rate limit con dos cupos en memoria (bucket4j)**: uno global (todos los usuarios) y uno por
  usuario autenticado, ambos configurables vía `app.rate-limit.*`. El default (5/min global,
  2/min por usuario) refleja el RPM=5 de ADR 0009 -- la restricción más probable de pisar en una
  sesión de evaluación corta, no el RPD=20 diario (para eso ya existe el failover automático a
  Groq de ADR 0009/E05.2). Si el cupo de un usuario alcanza pero el global ya se agotó, se le
  devuelve el token: no fue él quien vació la cuota compartida.
- **Filter, no `@ExceptionHandler`**: `ChatRateLimitFilter` corta la request antes de que llegue
  al controller, registrado en la cadena de `SecurityConfig` justo después de
  `JwtAuthenticationFilter` (necesita el usuario autenticado, pero tiene que decidir antes de que
  `ChatController#chatStream` abra el `SseEmitter` -- ahí ya no se puede reescribir el status
  HTTP, el mismo problema que ADR 0010 documentó para los errores del proveedor). Responde 429
  con un `ProblemDetail` (mismo formato que ya usa `BookingExceptionHandler`) y un header
  `Retry-After`.
- **`/actuator/health` público, con el detalle de la base visible**: se agrega un
  `permitAll()` en `SecurityConfig` para `GET /actuator/health` y sus subrutas, y
  `management.endpoint.health.show-details: always`. El `DataSourceHealthIndicator` de Spring
  Boot ya se auto-configura con `spring-boot-starter-actuator` + `spring-boot-starter-data-jpa`
  en el classpath -- no hace falta un indicador propio, solo exponer el detalle que ya calcula.

## Alternativas descartadas
- **Bucket4j-Redis / rate limit distribuido** -- descartado: el deploy es un único proceso
  (Railway, sin autoscaling en el alcance de este proyecto). Coordinar cupos entre instancias es
  complejidad que no compra nada hoy; si el deploy pasa a múltiples instancias, este ADR queda
  obsoleto y hay que revisitarlo.
- **Un solo cupo global (sin por-usuario)** -- descartado: no evita que un único usuario con una
  conversación larga se lleve toda la cuota, que es exactamente el escenario que motiva este
  issue.
- **Rate limit en el `@RestControllerAdvice`** -- descartado: no cubre `chatStream`, que ya
  compromete la respuesta como `200 text/event-stream` antes de que una excepción pueda cambiar el
  status (mismo motivo que ADR 0010 usa eventos in-band para los errores del proveedor ahí).
- **`show-details: when-authorized`** -- descartado por ahora: exigiría un rol admin sobre un
  endpoint que necesita ser público para los monitores externos, y el detalle expuesto (up/down de
  Postgres) no es sensible en este proyecto.

## Consecuencias
Un usuario que manda mensajes muy rápido, o una demo con varios evaluadores en paralelo, ve un 429
entendible en vez de agotar la cuota de Gemini en silencio y romper el chat para todos con un 500 o
un 503 más tarde. El cupo vive en memoria del proceso: un restart lo reinicia y no protege nada
entre instancias -- aceptable mientras el deploy sea de una sola instancia (ver alternativas
descartadas). `/actuator/health` queda alcanzable sin autenticar, incluyendo el estado de la base
de datos -- superficie de más para cualquiera que le pegue, pero acotada a "¿está viva la base?",
no a datos de negocio.
