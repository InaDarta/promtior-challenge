package com.promtior.booking.infrastructure.llm;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.function.Function;

/** {@link ChatModel} de prueba que delega en una función, sin credenciales ni red de por medio. */
record StubChatModel(Function<ChatRequest, ChatResponse> handler) implements ChatModel {
  @Override
  public ChatResponse doChat(ChatRequest request) {
    return handler.apply(request);
  }
}
