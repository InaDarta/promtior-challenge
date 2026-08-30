package com.promtior.booking.infrastructure.llm;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.util.List;
import java.util.function.Consumer;

/**
 * Decora el {@link TokenStream} real para cerrar el span de la conversación que abrió {@link
 * TracingBookingAssistant#chatStream} cuando el streaming termina -- en {@code onCompleteResponse}
 * o en {@code onError}, lo que llegue primero (LangChain4j garantiza uno solo de los dos por
 * stream) -- y para que ese span quede como contexto ambiente durante {@link #start()}, la única
 * ventana síncrona en la que {@link TracingChatModelListener#onRequest} puede capturarlo como padre
 * antes de que el proveedor despache la llamada de forma asíncrona (ver ADR 0011).
 */
class TracingTokenStream implements TokenStream {

  private final TokenStream delegate;
  private final Span span;
  private final Context context;
  private final String memoryId;
  private final ConversationTraceRegistry traceRegistry;

  TracingTokenStream(
      TokenStream delegate,
      Span span,
      Context context,
      String memoryId,
      ConversationTraceRegistry traceRegistry) {
    this.delegate = delegate;
    this.span = span;
    this.context = context;
    this.memoryId = memoryId;
    this.traceRegistry = traceRegistry;
  }

  @Override
  public TokenStream onPartialResponse(Consumer<String> consumer) {
    delegate.onPartialResponse(consumer);
    return this;
  }

  @Override
  public TokenStream onRetrieved(Consumer<List<Content>> consumer) {
    delegate.onRetrieved(consumer);
    return this;
  }

  @Override
  public TokenStream onToolExecuted(Consumer<ToolExecution> consumer) {
    delegate.onToolExecuted(consumer);
    return this;
  }

  @Override
  public TokenStream onCompleteResponse(Consumer<ChatResponse> consumer) {
    delegate.onCompleteResponse(
        response -> {
          finalizarSpan(response.aiMessage() == null ? null : response.aiMessage().text(), null);
          consumer.accept(response);
        });
    return this;
  }

  @Override
  public TokenStream onError(Consumer<Throwable> consumer) {
    delegate.onError(
        error -> {
          finalizarSpan(null, error);
          consumer.accept(error);
        });
    return this;
  }

  @Override
  public TokenStream ignoreErrors() {
    delegate.ignoreErrors();
    return this;
  }

  @Override
  public void start() {
    try (Scope scope = context.makeCurrent()) {
      delegate.start();
    }
  }

  private void finalizarSpan(String respuesta, Throwable error) {
    if (respuesta != null) {
      span.setAttribute("langfuse.observation.output", respuesta);
    }
    if (error != null) {
      span.recordException(error);
      span.setStatus(StatusCode.ERROR, String.valueOf(error.getMessage()));
    }
    span.end();
    traceRegistry.close(memoryId);
  }
}
