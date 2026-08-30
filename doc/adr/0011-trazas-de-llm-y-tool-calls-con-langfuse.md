# 0011. Trazas de LLM y tool calls con Langfuse

## Estado
Aceptada (2026-08-28)

## Contexto
[E07.4](https://github.com/InaDarta/promtior-challenge/issues/44), diferencial opcional de E07 ("no
bloquea la entrega"): mostrar que el agente es observable, con una conversación completa visible
como traza y sus tool calls anidados. Langfuse no publica un SDK Java propio -- solo Python y
JS/TS -- así que la única vía soportada para un stack Java/Spring es su [endpoint
OTLP](https://langfuse.com/integrations/native/opentelemetry), que ingiere trazas de OpenTelemetry
estándar. La app tampoco usa Quarkus (que sí trae instrumentación de LangChain4j lista vía
`quarkus-langchain4j`), así que esa integración no aplica: hay que armar los spans a mano.

LangChain4j 1.0 expone dos puntos de extensión relevantes: `ChatModelListener` (hooks
`onRequest`/`onResponse`/`onError` en cada `ChatModel`/`StreamingChatModel`) y `ToolExecutor` (la
interfaz detrás de cada método `@Tool`, reemplazable pasando un `Map<ToolSpecification,
ToolExecutor>` a `AiServices.tools(...)` en vez de los objetos de tools tal cual). Ninguno de los
dos alcanza solo: el listener nunca recibe el `memoryId` de la conversación, y el `ToolExecutor` no
tiene acceso al span de generación que disparó la tool call.

El otro problema es el camino de streaming (`chatStream`, ADR 0010): la respuesta y la ejecución de
tools posteriores corren en el hilo del cliente HTTP del proveedor, no en el que abrió la
conversación, así que el contexto ambiente de OTel (propagación por `ThreadLocal`) no alcanza para
anidar los spans correctamente.

## Decisión
Se instrumenta con OpenTelemetry vía Spring Boot: `micrometer-tracing-bridge-otel` +
`opentelemetry-exporter-otlp`, configurados contra el endpoint OTLP de Langfuse
(`management.otlp.tracing.*`) con Basic Auth. Sin código de bootstrap manual del SDK de OTel --
Spring Boot arma el `Tracer` y el exportador solo con esas properties.

Tres piezas de instrumentación manual, todas en `infrastructure.llm`, todas silenciosas ante
cualquier excepción propia (un bug de tracing nunca puede romper una conversación real):

- **`TracingBookingAssistant`** decora el `BookingAssistant` de `BookingAssistantConfig`: abre el
  span raíz ("agent") de cada turno, con `memoryId` como `session.id` y `user.id` de Langfuse.
- **`TracingChatModelListener`**, registrado en cada bean de `ChatModelConfig` (gemini, groq,
  ollama, sync y streaming; también en el primario y el respaldo de `FailoverChatModel`): abre el
  span "generation" en `onRequest` -- en ese momento el hilo es siempre el que abrió la
  conversación, antes de cualquier despacho asíncrono -- y lo cierra en `onResponse`/`onError`,
  venga de donde venga ese callback. El span en sí (no un `Context`, que sí depende del hilo) viaja
  entre los tres callbacks a través del mapa `attributes` que LangChain4j ya comparte entre ellos.
- **`TracingToolExecutor`** envuelve el `ToolExecutor` real de cada `@Tool` (armado a mano en
  `BookingAssistantConfig` vía `ToolSpecifications`/`DefaultToolExecutor`, en vez de
  `.tools(Object...)`) para abrir un span "tool". Su padre sale de `ConversationTraceRegistry` --
  un mapa `memoryId -> Context` que `TracingBookingAssistant` completa al abrir el turno -- no del
  contexto ambiente, que en streaming pertenece al hilo equivocado.

Todo detrás de `app.observability.langfuse.enabled` (default `false`, variable
`LANGFUSE_TRACING_ENABLED`): apagado, ningún componente arma un span -- no alcanza con deshabilitar
solo el exportador, porque igual se pagaría el costo de serializar prompts y argumentos que nadie va
a leer.

## Alternativas descartadas
- **Esperar a que LangChain4j tenga una integración nativa con Langfuse** -- descartada: no existe
  en la versión 1.0 usada por el proyecto (ADR 0001), y el issue es opcional pero tiene fecha de
  entrega fija.
- **Instrumentar solo el camino síncrono (`/api/chat`) e ignorar streaming** -- descartada: el
  criterio de aceptación pide una conversación completa como traza, y `/api/chat/stream` es el
  endpoint que realmente usa la UI (E06.3). El costo extra de propagar contexto explícitamente
  (`ConversationTraceRegistry`) fue chico comparado con dejar la mitad del tráfico real sin trazar.
- **Un Collector de OpenTelemetry como sidecar** -- descartado: agrega una pieza de infraestructura
  más para desplegar y operar (Railway, ADR pendiente de E08) a cambio de nada que Spring Boot no dé
  ya de forma directa contra el endpoint OTLP de Langfuse.

## Consecuencias
Correr esto en producción requiere `LANGFUSE_TRACING_ENABLED=true`, `LANGFUSE_OTEL_AUTH_HEADER`
("Basic " + base64 de `public_key:secret_key`, precalculado -- nunca las keys sueltas) y
opcionalmente `LANGFUSE_OTEL_ENDPOINT` si no es la región EU. Sin esas variables, la app se
comporta exactamente igual que antes de E07.4: cero spans, cero overhead, cero llamadas de red
nuevas.

`gen_ai.system` para Groq queda como `"open_ai"` (LangChain4j lo expone vía el cliente
OpenAI-compatible que usa Groq, ADR 0001): el modelo (`gen_ai.request.model`) es lo que realmente
distingue un proveedor de otro en el dashboard de Langfuse, no ese atributo.

`ConversationTraceRegistry` asume un solo turno en vuelo por `memoryId` a la vez -- cierto en el uso
real de la app, no bajo un cliente que mande dos mensajes en simultáneo bajo el mismo usuario; ese
caso no está cubierto y, en el peor caso, deja un span de tool sin padre correcto, no rompe la
conversación.
