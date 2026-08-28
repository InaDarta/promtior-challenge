package com.promtior.booking.infrastructure.llm;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delega en {@code primary} y recién ante un error transitorio (ver {@link TransientLlmErrors})
 * pasa a {@code fallback} en la misma llamada -- la salida automática que exige ADR 0009, no un
 * simple bean por perfil que haya que cambiar a mano.
 */
class FailoverChatModel implements ChatModel {

  private static final Logger log = LoggerFactory.getLogger(FailoverChatModel.class);

  private final ChatModel primary;
  private final ChatModel fallback;

  FailoverChatModel(ChatModel primary, ChatModel fallback) {
    this.primary = primary;
    this.fallback = fallback;
  }

  @Override
  public ChatResponse doChat(ChatRequest request) {
    try {
      return primary.chat(request);
    } catch (RuntimeException e) {
      if (!TransientLlmErrors.isTransient(e)) {
        throw e;
      }
      log.warn("Proveedor primario no disponible, usando el de respaldo", e);
      return fallback.chat(request);
    }
  }
}
