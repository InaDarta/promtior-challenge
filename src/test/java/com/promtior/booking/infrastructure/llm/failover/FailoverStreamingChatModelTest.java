package com.promtior.booking.infrastructure.llm.failover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.promtior.booking.infrastructure.llm.StubStreamingChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class FailoverStreamingChatModelTest {

  private static final ChatRequest REQUEST =
      ChatRequest.builder().messages(UserMessage.from("hola")).build();

  @Test
  void siElPrimarioRespondeNuncaConsultaAlDeRespaldo() {
    StreamingChatModel primary = tokensThenComplete("desde", "gemini");
    StreamingChatModel fallback =
        throwingBeforeAnyToken(() -> new IllegalStateException("no debería consultarse"));

    RecordingHandler handler = new RecordingHandler();
    new FailoverStreamingChatModel(primary, fallback).doChat(REQUEST, handler);

    assertEquals(List.of("desde", "gemini"), handler.tokens);
    assertNull(handler.error);
  }

  @Test
  void anteRateLimitExceptionAntesDeCualquierTokenDelegaAlDeRespaldo() {
    StreamingChatModel primary =
        throwingBeforeAnyToken(() -> new RateLimitException("cupo agotado"));
    StreamingChatModel fallback = tokensThenComplete("desde", "groq");

    RecordingHandler handler = new RecordingHandler();
    new FailoverStreamingChatModel(primary, fallback).doChat(REQUEST, handler);

    assertEquals(List.of("desde", "groq"), handler.tokens);
    assertNull(handler.error);
  }

  @Test
  void anteElHttpErrorSinTiparDelClienteBetaDeGeminiAntesDeCualquierTokenDelegaAlDeRespaldo() {
    StreamingChatModel primary =
        throwingBeforeAnyToken(
            () -> new RuntimeException("HTTP error (503): This model is currently overloaded"));
    StreamingChatModel fallback = tokensThenComplete("desde", "groq");

    RecordingHandler handler = new RecordingHandler();
    new FailoverStreamingChatModel(primary, fallback).doChat(REQUEST, handler);

    assertEquals(List.of("desde", "groq"), handler.tokens);
    assertNull(handler.error);
  }

  @Test
  void anteUnaFallaDeRedAntesDeCualquierTokenDelegaAlDeRespaldo() {
    StreamingChatModel primary =
        throwingBeforeAnyToken(
            () ->
                new RuntimeException(
                    "An error occurred while sending the request",
                    new IOException("conexión rechazada")));
    StreamingChatModel fallback = tokensThenComplete("desde", "groq");

    RecordingHandler handler = new RecordingHandler();
    new FailoverStreamingChatModel(primary, fallback).doChat(REQUEST, handler);

    assertEquals(List.of("desde", "groq"), handler.tokens);
    assertNull(handler.error);
  }

  @Test
  void unErrorTransitorioDespuesDeEmitirTokensNoHaceFailoverYSePropaga() {
    RuntimeException errorAMitadDeCamino = new RateLimitException("cupo agotado a mitad de camino");
    StreamingChatModel primary =
        tokensThenError(List.of("desde", "gemini"), () -> errorAMitadDeCamino);
    StreamingChatModel fallback =
        throwingBeforeAnyToken(() -> new IllegalStateException("no debería consultarse"));

    RecordingHandler handler = new RecordingHandler();
    new FailoverStreamingChatModel(primary, fallback).doChat(REQUEST, handler);

    assertEquals(List.of("desde", "gemini"), handler.tokens);
    assertSame(errorAMitadDeCamino, handler.error);
  }

  @Test
  void anteUnErrorNoTransitorioNoDelegaYPropagaLaExcepcionOriginal() {
    RuntimeException apiKeyInvalida = new RuntimeException("HTTP error (401): API key inválida");
    StreamingChatModel primary = throwingBeforeAnyToken(() -> apiKeyInvalida);
    StreamingChatModel fallback =
        throwingBeforeAnyToken(() -> new IllegalStateException("no debería consultarse"));

    RecordingHandler handler = new RecordingHandler();
    new FailoverStreamingChatModel(primary, fallback).doChat(REQUEST, handler);

    assertSame(apiKeyInvalida, handler.error);
  }

  private static StreamingChatModel tokensThenComplete(String... tokens) {
    return new StubStreamingChatModel(
        (request, handler) -> {
          for (String token : tokens) {
            handler.onPartialResponse(token);
          }
          handler.onCompleteResponse(
              ChatResponse.builder().aiMessage(AiMessage.from(String.join(" ", tokens))).build());
        });
  }

  private static StreamingChatModel tokensThenError(
      List<String> tokens, Supplier<RuntimeException> exceptionSupplier) {
    return new StubStreamingChatModel(
        (request, handler) -> {
          tokens.forEach(handler::onPartialResponse);
          handler.onError(exceptionSupplier.get());
        });
  }

  private static StreamingChatModel throwingBeforeAnyToken(
      Supplier<RuntimeException> exceptionSupplier) {
    return new StubStreamingChatModel(
        (request, handler) -> handler.onError(exceptionSupplier.get()));
  }

  private static class RecordingHandler implements StreamingChatResponseHandler {

    final List<String> tokens = new ArrayList<>();
    Throwable error;

    @Override
    public void onPartialResponse(String partialResponse) {
      tokens.add(partialResponse);
    }

    @Override
    public void onCompleteResponse(ChatResponse response) {}

    @Override
    public void onError(Throwable error) {
      this.error = error;
    }
  }
}
