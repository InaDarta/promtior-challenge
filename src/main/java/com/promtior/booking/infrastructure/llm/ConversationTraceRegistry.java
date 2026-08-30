package com.promtior.booking.infrastructure.llm;

import io.opentelemetry.context.Context;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Guarda, por {@code memoryId}, el {@link Context} de OTel del turno de conversación en curso -- lo
 * único que {@link TracingToolExecutor} necesita para que el span de una tool cuelgue del de la
 * conversación en vez de quedar huérfano (ver ADR 0011).
 *
 * <p>Hace falta este registro porque el contexto ambiente de OTel no alcanza en el camino de
 * streaming: {@link TracingBookingAssistant} abre el span de la conversación en el hilo del
 * request, pero la ejecución de una tool durante {@code chatStream} corre en el hilo del cliente
 * HTTP del proveedor -- uno nuevo por turno, sin relación con el que abrió la conversación -- así
 * que ese hilo no ve el contexto ambiente y sin este mapa el span de la tool quedaría sin padre. En
 * el camino síncrono el contexto ambiente ya alcanza (todo corre en el mismo hilo); consultar acá
 * igual no cambia el resultado.
 *
 * <p>Un {@code memoryId} tiene un solo turno en vuelo a la vez en el uso real de la app -- nadie
 * manda dos mensajes en simultáneo bajo el mismo usuario -- así que un mapa simple alcanza; no hace
 * falta una pila por sesión.
 */
@Component
class ConversationTraceRegistry {

  private final Map<String, Context> contextoPorMemoryId = new ConcurrentHashMap<>();

  void open(String memoryId, Context context) {
    contextoPorMemoryId.put(memoryId, context);
  }

  Optional<Context> current(String memoryId) {
    return Optional.ofNullable(contextoPorMemoryId.get(memoryId));
  }

  void close(String memoryId) {
    contextoPorMemoryId.remove(memoryId);
  }
}
