# 0009. Límites del tier gratuito de Gemini y confirmación del proveedor primario

## Estado
Aceptada (2026-08-28)

## Contexto
Spike de de-riesgo [#32](https://github.com/InaDarta/promtior-challenge/issues/32), previo a escribir el
adaptador de proveedor (E05.2). ADR 0001 ya fijó Gemini como proveedor primario por su ventaja en tool
calling frente a Groq, pero sin verificar los límites de frecuencia vigentes del tier gratuito ni probar
una llamada real con function calling contra el modelo GA actual, `gemini-3.7-flash` (ver ADR 0002).

## Decisión
Se confirma Gemini (`gemini-3.7-flash`) como proveedor primario, con Groq como fallback documentado, tal
como fija ADR 0001 — pero con un límite de frecuencia mucho más ajustado de lo esperado, que queda
registrado como riesgo operativo a monitorear durante E05.

- **Key y tier**: la API key de Google AI Studio funciona — las llamadas de prueba llegaron hasta la capa
  de autenticación de Google (devolvieron `503`, no `401`/`403`). El proyecto sigue en "Nivel gratuito" sin
  facturación configurada: confirma que no hace falta tarjeta.
- **Límites vigentes** (panel `aistudio.google.com/rate-limit`, categoría "Modelos de texto de salida",
  compartidos entre `gemini-3.5-flash`, `gemini-3.6-flash` y `gemini-3.7-flash`):

  | Límite | Valor |
  |---|---|
  | RPM | 5 |
  | TPM | 250.000 |
  | RPD | 20 |

  Son sensiblemente más bajos que lo que sugerían fuentes de terceros (10-15 RPM, 500-1500 RPD según el
  sitio). Google ya no publica una tabla estática por modelo para el tier gratuito — el número real solo
  se ve en el panel de la cuenta, y solo después de tener uso registrado.
- **Llamada con function calling**: 13 intentos contra `gemini-3.7-flash` a lo largo de ~10 minutos (con y
  sin declaración de tool) devolvieron `503 UNAVAILABLE` — "This model is currently experiencing high
  demand" — de forma sostenida, no como blip aislado. El mismo error con y sin tools descarta un problema
  de schema o de la llamada en sí: es saturación del modelo, GA hace apenas dos semanas (13-08-2026). No
  se logró capturar una respuesta exitosa con `functionCall` durante el spike.
- **Detalle operativo importante**: los intentos fallidos con `503` igual consumieron cupo — el RPD pasó
  de 0 a 14/20 pese a que las 13 llamadas fallaron. Un reintento sin techo puede agotar el cupo diario en
  minutos.

Se mantiene Gemini como primario por ahora: la decisión de ADR 0001 se basa en calidad de tool calling, un
eje distinto de la disponibilidad puntual observada acá. Si en la práctica (durante la implementación de
E05 o la demo) el rate limit o la disponibilidad no alcanzan, se promueve Groq a primario — esa decisión
queda diferida, no resuelta en este spike.

## Alternativas descartadas
- **Promover Groq a primario ahora mismo** — descartado por ahora: el `503` observado es saturación de un
  modelo recién liberado, que Google señala como "usualmente temporal", no evidencia de que Gemini sea
  inviable. Cambiar el primario sobre la base de una ventana de 10 minutos sería reaccionar de más.
- **Reintentar con backoff hasta conseguir una respuesta exitosa** — descartado dentro del spike: cada
  reintento consume cupo de un RPD de apenas 20; seguir insistiendo el mismo día arriesgaba dejar la
  cuenta sin cupo. La lógica de reintento con backoff queda pendiente para la implementación real en
  E05.2, con un techo bajo de intentos.

## Consecuencias
Con RPD=20 y RPM=5, el tier gratuito de Gemini es sensiblemente más chico de lo que asumía el plan
original — insuficiente para una sesión de demo con varios turnos si cada turno dispara más de una
llamada (reintentos, o el propio LangChain4j reintentando internamente). E05.2 tiene que implementar el
failover a Groq contemplando esto explícitamente, no solo como "otro perfil de Spring" sino como salida
automática ante `503` o agotamiento de cuota. Cualquier lógica de reintento debe tener un techo bajo (2-3
intentos como mucho) para no quemar el cupo diario en una sola conversación. Queda pendiente, fuera del
alcance de este spike, probar Groq en vivo — se hace si el rate limit de Gemini demuestra ser insuficiente
en la práctica.
