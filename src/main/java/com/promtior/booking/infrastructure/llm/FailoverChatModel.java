package com.promtior.booking.infrastructure.llm;

import dev.langchain4j.exception.RetriableException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delega en {@code primary} y recién ante un error transitorio (503, 429, red caída) pasa a {@code
 * fallback} en la misma llamada -- la salida automática que exige ADR 0009, no un simple bean por
 * perfil que haya que cambiar a mano.
 *
 * <p>El cliente beta de Gemini ({@code langchain4j-google-ai-gemini}) no usa la jerarquía de
 * excepciones de {@code langchain4j-core}: envuelve cualquier error HTTP en un {@link
 * RuntimeException} con el status en el mensaje ({@code "HTTP error (503): ..."}), a diferencia del
 * cliente de Groq (OpenAI-compatible, ya en GA) que sí lanza {@link RetriableException}. Por eso
 * {@link #isTransient(RuntimeException)} cubre ambos casos.
 */
class FailoverChatModel implements ChatModel {

  private static final Logger log = LoggerFactory.getLogger(FailoverChatModel.class);
  private static final Pattern HTTP_ERROR_STATUS = Pattern.compile("HTTP error \\((\\d{3})\\)");

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
      if (!isTransient(e)) {
        throw e;
      }
      log.warn("Proveedor primario no disponible, usando el de respaldo", e);
      return fallback.chat(request);
    }
  }

  private static boolean isTransient(RuntimeException e) {
    if (e instanceof RetriableException) {
      return true;
    }
    if (e.getCause() instanceof IOException) {
      return true;
    }
    Matcher matcher = HTTP_ERROR_STATUS.matcher(String.valueOf(e.getMessage()));
    if (!matcher.find()) {
      return false;
    }
    int status = Integer.parseInt(matcher.group(1));
    return status == 429 || status >= 500;
  }
}
