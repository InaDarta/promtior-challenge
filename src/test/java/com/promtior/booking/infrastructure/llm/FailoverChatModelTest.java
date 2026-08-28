package com.promtior.booking.infrastructure.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.io.IOException;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class FailoverChatModelTest {

  private static final ChatRequest REQUEST =
      ChatRequest.builder().messages(UserMessage.from("hola")).build();

  @Test
  void siElPrimarioRespondeNuncaConsultaAlDeRespaldo() {
    ChatModel primary = fixedResponse("desde gemini");
    ChatModel fallback = throwing(() -> new IllegalStateException("no debería consultarse"));

    ChatModel failover = new FailoverChatModel(primary, fallback);

    assertEquals("desde gemini", failover.chat(REQUEST).aiMessage().text());
  }

  @Test
  void anteRateLimitExceptionDelegaAlDeRespaldo() {
    ChatModel primary = throwing(() -> new RateLimitException("cupo agotado"));
    ChatModel fallback = fixedResponse("desde groq");

    ChatModel failover = new FailoverChatModel(primary, fallback);

    assertEquals("desde groq", failover.chat(REQUEST).aiMessage().text());
  }

  @Test
  void anteElHttpErrorSinTiparDelClienteBetaDeGeminiDelegaAlDeRespaldo() {
    ChatModel primary =
        throwing(
            () -> new RuntimeException("HTTP error (503): This model is currently overloaded"));
    ChatModel fallback = fixedResponse("desde groq");

    ChatModel failover = new FailoverChatModel(primary, fallback);

    assertEquals("desde groq", failover.chat(REQUEST).aiMessage().text());
  }

  @Test
  void anteUnaFallaDeRedDelegaAlDeRespaldo() {
    ChatModel primary =
        throwing(
            () ->
                new RuntimeException(
                    "An error occurred while sending the request",
                    new IOException("conexión rechazada")));
    ChatModel fallback = fixedResponse("desde groq");

    ChatModel failover = new FailoverChatModel(primary, fallback);

    assertEquals("desde groq", failover.chat(REQUEST).aiMessage().text());
  }

  @Test
  void anteUnErrorNoTransitorioNoDelegaYPropagaLaExcepcionOriginal() {
    RuntimeException apiKeyInvalida = new RuntimeException("HTTP error (401): API key inválida");
    ChatModel primary = throwing(() -> apiKeyInvalida);
    ChatModel fallback = throwing(() -> new IllegalStateException("no debería consultarse"));

    ChatModel failover = new FailoverChatModel(primary, fallback);

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> failover.chat(REQUEST));
    assertSame(apiKeyInvalida, thrown);
  }

  private static ChatModel fixedResponse(String text) {
    return new StubChatModel(
        request -> ChatResponse.builder().aiMessage(AiMessage.from(text)).build());
  }

  private static ChatModel throwing(Supplier<RuntimeException> exceptionSupplier) {
    return new StubChatModel(
        request -> {
          throw exceptionSupplier.get();
        });
  }
}
