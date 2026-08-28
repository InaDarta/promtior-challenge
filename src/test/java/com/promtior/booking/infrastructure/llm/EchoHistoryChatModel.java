package com.promtior.booking.infrastructure.llm;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * {@link ChatModel} de prueba que responde con la concatenación de los mensajes de usuario que trae
 * el request -- permite verificar, sin un proveedor real de por medio, que el historial de
 * conversación por {@code memoryId} le llega al modelo en cada turno.
 */
public class EchoHistoryChatModel implements ChatModel {

  @Override
  public ChatResponse doChat(ChatRequest request) {
    String historial =
        request.messages().stream()
            .filter(UserMessage.class::isInstance)
            .map(m -> ((UserMessage) m).singleText())
            .reduce((a, b) -> a + " " + b)
            .orElse("");
    return ChatResponse.builder().aiMessage(AiMessage.from(historial)).build();
  }
}
