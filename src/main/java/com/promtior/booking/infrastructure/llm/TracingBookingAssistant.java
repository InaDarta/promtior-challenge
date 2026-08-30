package com.promtior.booking.infrastructure.llm;

import dev.langchain4j.service.TokenStream;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decora el {@link BookingAssistant} que arma {@link BookingAssistantConfig} con un span "agent" de
 * Langfuse por turno de conversación -- la raíz de la traza de la que cuelgan, como hermanos, los
 * spans "generation" de {@link TracingChatModelListener} y "tool" de {@link TracingToolExecutor}
 * (ver ADR 0011).
 *
 * <p>{@code memoryId} hace de {@code sessionId} y de {@code userId} de Langfuse: en esta app es
 * siempre el username del usuario autenticado (ver {@link
 * com.promtior.booking.infrastructure.rest.ChatController}), así que agrupar por ese valor ya
 * agrupa por sesión y por usuario a la vez.
 */
class TracingBookingAssistant implements BookingAssistant {

  private static final Logger log = LoggerFactory.getLogger(TracingBookingAssistant.class);

  private final BookingAssistant delegate;
  private final Tracer tracer;
  private final LangfuseProperties properties;
  private final ConversationTraceRegistry traceRegistry;

  TracingBookingAssistant(
      BookingAssistant delegate,
      Tracer tracer,
      LangfuseProperties properties,
      ConversationTraceRegistry traceRegistry) {
    this.delegate = delegate;
    this.tracer = tracer;
    this.properties = properties;
    this.traceRegistry = traceRegistry;
  }

  @Override
  public String chat(String memoryId, String message) {
    if (!properties.enabled()) {
      return delegate.chat(memoryId, message);
    }

    Span span = abrirSpanDeTurno(memoryId, "chat", message);
    Context context = span.storeInContext(Context.current());
    traceRegistry.open(memoryId, context);
    try (Scope scope = context.makeCurrent()) {
      String respuesta = delegate.chat(memoryId, message);
      span.setAttribute("langfuse.observation.output", respuesta);
      return respuesta;
    } catch (RuntimeException e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR, String.valueOf(e.getMessage()));
      throw e;
    } finally {
      span.end();
      traceRegistry.close(memoryId);
    }
  }

  @Override
  public TokenStream chatStream(String memoryId, String message) {
    if (!properties.enabled()) {
      return delegate.chatStream(memoryId, message);
    }

    Span span = abrirSpanDeTurno(memoryId, "chat-stream", message);
    Context context = span.storeInContext(Context.current());
    traceRegistry.open(memoryId, context);
    TokenStream real;
    try (Scope scope = context.makeCurrent()) {
      real = delegate.chatStream(memoryId, message);
    } catch (RuntimeException e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR, String.valueOf(e.getMessage()));
      span.end();
      traceRegistry.close(memoryId);
      throw e;
    }
    return new TracingTokenStream(real, span, context, memoryId, traceRegistry);
  }

  private Span abrirSpanDeTurno(String memoryId, String nombre, String mensaje) {
    Span span;
    try {
      span = tracer.spanBuilder(nombre).startSpan();
      span.setAttribute("langfuse.observation.type", "agent");
      span.setAttribute("langfuse.trace.name", nombre);
      span.setAttribute("langfuse.session.id", memoryId);
      span.setAttribute("langfuse.user.id", memoryId);
      span.setAttribute("langfuse.observation.input", mensaje);
    } catch (RuntimeException e) {
      log.warn("No se pudo abrir el span de la conversación", e);
      span = Span.getInvalid();
    }
    return span;
  }
}
