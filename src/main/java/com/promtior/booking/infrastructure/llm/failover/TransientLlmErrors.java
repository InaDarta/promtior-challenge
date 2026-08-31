package com.promtior.booking.infrastructure.llm.failover;

import dev.langchain4j.exception.RetriableException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detecta si un error de proveedor de LLM es transitorio (503, 429, red caída) y por lo tanto vale
 * la pena reintentar contra un proveedor de respaldo -- usado tanto por {@link FailoverChatModel}
 * (síncrono) como por {@link FailoverStreamingChatModel}, para que ambos caminos coincidan en qué
 * cuenta como transitorio.
 *
 * <p>El cliente beta de Gemini ({@code langchain4j-google-ai-gemini}) no usa la jerarquía de
 * excepciones de {@code langchain4j-core}: envuelve cualquier error HTTP en un {@link
 * RuntimeException} con el status en el mensaje ({@code "HTTP error (503): ..."}), a diferencia del
 * cliente de Groq (OpenAI-compatible, ya en GA) que sí lanza {@link RetriableException}. Por eso
 * {@link #isTransient(Throwable)} cubre ambos casos.
 */
final class TransientLlmErrors {

  private static final Pattern HTTP_ERROR_STATUS = Pattern.compile("HTTP error \\((\\d{3})\\)");

  private TransientLlmErrors() {}

  static boolean isTransient(Throwable e) {
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
