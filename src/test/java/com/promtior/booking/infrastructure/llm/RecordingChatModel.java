package com.promtior.booking.infrastructure.llm;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Envoltorio de un {@link ChatModel} real que graba, en orden, cada {@link ToolExecutionRequest}
 * que el modelo dispara -- tal cual las emite, sin coercionar sus argumentos -- sin alterar en nada
 * la respuesta. Ni {@code AiServices} ni {@link BookingAssistant} exponen esa decisión intermedia
 * del loop de tool calling; envolver el {@code ChatModel} es el único punto donde {@link
 * BookingAgentEvalRunner} puede observarla desde afuera.
 *
 * <p>Delega en {@link #chat(ChatRequest)}, no en {@code doChat}: {@code ChatModel.doChat} es un
 * método default que solo tira {@code RuntimeException("Not implemented")} salvo que la
 * implementación concreta lo pise -- las de langchain4j (p.ej. {@code GoogleAiGeminiChatModel})
 * implementan directamente {@code chat(ChatRequest)}, sin pisar {@code doChat}. Delegar en {@code
 * doChat} acá caía siempre en ese default, sin tocar la red ni la key -- por más real que fuera el
 * {@link ChatModel} envuelto.
 */
final class RecordingChatModel implements ChatModel {

  private final ChatModel delegate;
  private final List<ToolExecutionRequest> llamadas = new ArrayList<>();

  RecordingChatModel(ChatModel delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  @Override
  public ChatResponse chat(ChatRequest request) {
    ChatResponse response = delegate.chat(request);
    if (response.aiMessage().hasToolExecutionRequests()) {
      llamadas.addAll(response.aiMessage().toolExecutionRequests());
    }
    return response;
  }

  /** Posición actual, para aislar después las tool calls de un turno puntual con {@link #desde}. */
  int marca() {
    return llamadas.size();
  }

  /** Tool calls grabadas desde la {@code marca} dada (inclusive) hasta ahora, en orden. */
  List<ToolExecutionRequest> desde(int marca) {
    return List.copyOf(llamadas.subList(marca, llamadas.size()));
  }
}
