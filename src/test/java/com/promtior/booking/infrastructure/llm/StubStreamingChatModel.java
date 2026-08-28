package com.promtior.booking.infrastructure.llm;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import java.util.function.BiConsumer;

/**
 * {@link StreamingChatModel} de prueba que delega en una función, sin credenciales ni red de por
 * medio -- equivalente en streaming de {@link StubChatModel}. Los callbacks corren de forma
 * síncrona, en el mismo hilo que invoca {@link #doChat}.
 */
record StubStreamingChatModel(BiConsumer<ChatRequest, StreamingChatResponseHandler> behavior)
    implements StreamingChatModel {

  @Override
  public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
    behavior.accept(request, handler);
  }
}
