# 0010. Streaming del chat: errores in-band, `fetch` en vez de `EventSource`, failover acotado

## Estado
Aceptada (2026-08-28)

## Contexto
E06.3 ([#40](https://github.com/InaDarta/promtior-challenge/issues/40)) agrega `POST /api/chat/stream`
para que la espera del modelo no se sienta como un cuelgue, en paralelo al `POST /api/chat` síncrono de
E05.3 (que queda intacto). Tres decisiones tienen una alternativa descartada real y quedan registradas
acá.

## Decisión
1. **Errores del stream como evento SSE `error`, no como status HTTP.** Una vez que el controller
   devuelve el `SseEmitter`, Spring ya comprometió la respuesta como `200 text/event-stream`. Para
   cuando `LlmNotConfiguredException` puede ocurrir (al resolver el proveedor real dentro del callback
   async de `StreamingChatModel`, no al invocar el método del controller), ya no hay forma de reescribir
   el status -- a diferencia de `POST /api/chat`, que sí puede responder `503` porque falla antes de
   escribir nada. El vocabulario de eventos queda: `token` (fragmento parcial), `done` (texto final
   completo, no solo una marca de fin -- el cliente lo usa para reemplazar lo acumulado por los tokens),
   `error` (mensaje en español).
2. **El frontend consume el stream con `fetch` + `ReadableStream`, no con `EventSource` nativo.** La
   autenticación del proyecto es `Authorization: Bearer <jwt>` guardado en `sessionStorage` (ver ADR
   0006), nunca una cookie. `EventSource` no permite mandar headers custom; la única forma de autenticar
   una petición `EventSource` sería poner el token en la URL como query param, algo que expondría el JWT
   en logs de acceso y en el historial del navegador. `fetch` sí permite mandar el header tal cual ya lo
   hace el resto de la API.
3. **El failover Gemini→Groq de streaming (ADR 0009) solo actúa antes del primer token emitido.** Una
   vez que el cliente ya recibió texto del proveedor primario, reintentar contra el de respaldo
   produciría una respuesta con tokens duplicados o intercalados de dos proveedores distintos -- no hay
   forma de "deshacer" texto que el navegador ya renderizó. Si el error transitorio llega después del
   primer token, se propaga tal cual como evento `error`.

## Alternativas descartadas
- **Devolver 503 igual, cancelando el emitter antes de que el cliente vea el `200`** — no es posible con
  `SseEmitter`/Spring MVC: el status y los headers se comprometen al entrar en procesamiento async, antes
  de que el proveedor de LLM se resuelva de forma diferida (mismo mecanismo que ya usa
  `BookingAssistantConfig.deferredChatModel` para el camino síncrono).
- **`EventSource` con el token como query param** — descartado por exponer el JWT en URLs (logs de
  acceso, historial del navegador), un compromiso de seguridad que el proyecto evita deliberadamente en
  todos los demás endpoints.
- **Buffer-and-replay: acumular los tokens del primario y recién "soltarlos" al cliente si el primario
  completa bien, permitiendo reintentar contra el respaldo aunque ya haya tokens generados** —
  descartado por complejidad (hay que retener el stream completo antes de reenviarlo, perdiendo la
  ventaja de latencia percibida que es la razón de ser de esta issue) para un caso límite (falla
  *después* de empezar a responder bien) que además es raro: los `503`/`429` que motivan el failover
  ocurren típicamente al primer contacto con el proveedor, no a mitad de una respuesta ya en curso.

## Consecuencias
El endpoint síncrono y el de streaming coexisten con contratos de error distintos (503 vs. evento
in-band) — cualquier cliente nuevo contra `/api/chat/stream` tiene que saber leer el vocabulario de
eventos, no alcanza con mirar el status HTTP. El failover de streaming cubre el caso que en la práctica
importa (cupo agotado o proveedor caído antes de empezar a responder, ver ADR 0009) pero no protege una
conversación que ya arrancó a responder y falla a mitad de camino -- ese caso se ve como una respuesta
cortada con un error visible, no como una silenciosa continuación desde otro proveedor.
