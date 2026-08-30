package com.promtior.booking.infrastructure.llm;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Envuelve un {@link ToolExecutor} de LangChain4j con un span "tool" de Langfuse -- {@link
 * BookingAssistantConfig} arma uno de estos por cada método {@code @Tool} en vez de registrar las
 * tools tal cual, para no tocar {@link RoomQueryTools}, {@link BookingQueryTools} ni {@link
 * BookingTools} (ADR 0011).
 *
 * <p>El padre del span sale de {@link ConversationTraceRegistry}, nunca del contexto ambiente de
 * OTel: en el camino de streaming, la ejecución de la tool corre en el hilo del cliente HTTP del
 * proveedor, no en el que abrió la conversación, así que el contexto ambiente de ese hilo no sirve.
 *
 * <p>Los argumentos y el resultado de la tool ({@link ToolExecutionRequest#arguments()} y lo que
 * devuelve {@link #execute}) ya son JSON -- se cargan tal cual en el span, sin volver a
 * serializarlos.
 */
class TracingToolExecutor implements ToolExecutor {

  private static final Logger log = LoggerFactory.getLogger(TracingToolExecutor.class);

  private final ToolExecutor delegate;
  private final String toolName;
  private final Tracer tracer;
  private final LangfuseProperties properties;
  private final ConversationTraceRegistry traceRegistry;

  TracingToolExecutor(
      ToolExecutor delegate,
      String toolName,
      Tracer tracer,
      LangfuseProperties properties,
      ConversationTraceRegistry traceRegistry) {
    this.delegate = delegate;
    this.toolName = toolName;
    this.tracer = tracer;
    this.properties = properties;
    this.traceRegistry = traceRegistry;
  }

  @Override
  public String execute(ToolExecutionRequest request, Object memoryId) {
    if (!properties.enabled()) {
      return delegate.execute(request, memoryId);
    }

    Span span;
    try {
      Context parent =
          memoryId instanceof String id
              ? traceRegistry.current(id).orElseGet(Context::current)
              : Context.current();
      span = tracer.spanBuilder(toolName).setParent(parent).startSpan();
      span.setAttribute("langfuse.observation.type", "tool");
      span.setAttribute("langfuse.observation.input", request.arguments());
    } catch (RuntimeException e) {
      log.warn("No se pudo abrir el span de la tool {}", toolName, e);
      return delegate.execute(request, memoryId);
    }

    try (Scope scope = span.makeCurrent()) {
      String result = delegate.execute(request, memoryId);
      span.setAttribute("langfuse.observation.output", result);
      return result;
    } catch (RuntimeException e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR, String.valueOf(e.getMessage()));
      throw e;
    } finally {
      span.end();
    }
  }
}
