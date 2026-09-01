package com.promtior.booking.infrastructure.llm;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

/**
 * Equivalente en streaming de {@link EchoHistoryChatModel}: responde con la concatenación de los
 * mensajes de usuario que trae el request, mandada palabra por palabra vía {@link
 * StreamingChatResponseHandler#onPartialResponse} antes de completar -- permite verificar el
 * historial de conversación por {@code memoryId} y el vocabulario de eventos SSE sin un proveedor
 * real de por medio.
 */
public class EchoHistoryStreamingChatModel implements StreamingChatModel {

  @Override
  public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
    String historial =
        request.messages().stream()
            .filter(UserMessage.class::isInstance)
            .map(m -> ((UserMessage) m).singleText())
            .reduce((a, b) -> a + " " + b)
            .orElse("");
    for (String palabra : historial.split(" ")) {
      handler.onPartialResponse(palabra + " ");
    }
    handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from(historial)).build());
  }
}
