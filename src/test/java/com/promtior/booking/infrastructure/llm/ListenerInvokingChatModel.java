package com.promtior.booking.infrastructure.llm;

import com.promtior.booking.infrastructure.llm.tracing.TracingChatModelListener;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ChatModel} de prueba que invoca un {@link ChatModelListener} exactamente como lo hacen los
 * builders reales de LangChain4j ({@code onRequest} antes de delegar, {@code onResponse}/{@code
 * onError} después, con el mismo mapa {@code attributes} en las tres llamadas): {@link
 * StubChatModel} no pasa por ningún builder de proveedor, así que sin este wrapper {@link
 * TracingChatModelListener} nunca se ejercitaría en un test determinista.
 */
public record ListenerInvokingChatModel(ChatModel delegate, ChatModelListener listener)
    implements ChatModel {

  @Override
  public ChatResponse doChat(ChatRequest request) {
    Map<Object, Object> attributes = new ConcurrentHashMap<>();
    listener.onRequest(new ChatModelRequestContext(request, ModelProvider.OTHER, attributes));
    try {
      ChatResponse response = delegate.chat(request);
      listener.onResponse(
          new ChatModelResponseContext(response, request, ModelProvider.OTHER, attributes));
      return response;
    } catch (RuntimeException e) {
      listener.onError(new ChatModelErrorContext(e, request, ModelProvider.OTHER, attributes));
      throw e;
    }
  }
}
