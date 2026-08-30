package com.promtior.booking.infrastructure.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Prueba el criterio de aceptación de E05.2: cambiar de proveedor es cambiar el perfil activo, y el
 * perfil {@code gemini} incluye el failover automático a Groq (ADR 0009). {@link
 * TracingChatModelListener} se provee con tracing apagado (E07.4): estos tests no ejercitan ninguna
 * llamada real al modelo, así que no hay nada que trazar.
 */
class ChatModelConfigTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(ChatModelConfig.class)
          .withBean(
              TracingChatModelListener.class,
              () ->
                  new TracingChatModelListener(
                      OpenTelemetry.noop().getTracer("test"),
                      new LangfuseProperties(false),
                      new ObjectMapper()))
          .withPropertyValues(
              "app.llm.gemini.api-key=gemini-key",
              "app.llm.gemini.model-name=gemini-3.7-flash",
              "app.llm.gemini.max-retries=2",
              "app.llm.groq.api-key=groq-key",
              "app.llm.groq.model-name=llama-3.3-70b-versatile",
              "app.llm.groq.base-url=https://api.groq.com/openai/v1",
              "app.llm.ollama.base-url=http://localhost:11434",
              "app.llm.ollama.model-name=llama3.1");

  @Test
  void conElPerfilGeminiActivoElBeanEsElFailoverHaciaGroq() {
    contextRunner
        .withPropertyValues("spring.profiles.active=gemini")
        .run(
            context -> {
              assertEquals(1, context.getBeansOfType(ChatModel.class).size());
              assertInstanceOf(FailoverChatModel.class, context.getBean(ChatModel.class));
            });
  }

  @Test
  void conElPerfilGroqActivoElBeanEsElClienteOpenAiCompatibleSinFailover() {
    contextRunner
        .withPropertyValues("spring.profiles.active=groq")
        .run(
            context -> {
              assertEquals(1, context.getBeansOfType(ChatModel.class).size());
              assertInstanceOf(OpenAiChatModel.class, context.getBean(ChatModel.class));
            });
  }

  @Test
  void conElPerfilOllamaActivoElBeanEsElClienteLocalSinFailover() {
    contextRunner
        .withPropertyValues("spring.profiles.active=ollama")
        .run(
            context -> {
              assertEquals(1, context.getBeansOfType(ChatModel.class).size());
              assertInstanceOf(OllamaChatModel.class, context.getBean(ChatModel.class));
            });
  }

  @Test
  void sinPerfilDeProveedorActivoNoSeCreaNingunChatModel() {
    contextRunner.run(
        context -> {
          assertTrue(context.getBeansOfType(ChatModel.class).isEmpty());
          assertTrue(context.getBeansOfType(StreamingChatModel.class).isEmpty());
        });
  }

  @Test
  void conElPerfilGeminiActivoElBeanDeStreamingEsElFailoverHaciaGroq() {
    contextRunner
        .withPropertyValues("spring.profiles.active=gemini")
        .run(
            context -> {
              assertEquals(1, context.getBeansOfType(StreamingChatModel.class).size());
              assertInstanceOf(
                  FailoverStreamingChatModel.class, context.getBean(StreamingChatModel.class));
            });
  }

  @Test
  void conElPerfilGroqActivoElBeanDeStreamingEsElClienteOpenAiCompatibleSinFailover() {
    contextRunner
        .withPropertyValues("spring.profiles.active=groq")
        .run(
            context -> {
              assertEquals(1, context.getBeansOfType(StreamingChatModel.class).size());
              assertInstanceOf(
                  OpenAiStreamingChatModel.class, context.getBean(StreamingChatModel.class));
            });
  }

  @Test
  void conElPerfilOllamaActivoElBeanDeStreamingEsElClienteLocalSinFailover() {
    contextRunner
        .withPropertyValues("spring.profiles.active=ollama")
        .run(
            context -> {
              assertEquals(1, context.getBeansOfType(StreamingChatModel.class).size());
              assertInstanceOf(
                  OllamaStreamingChatModel.class, context.getBean(StreamingChatModel.class));
            });
  }
}
