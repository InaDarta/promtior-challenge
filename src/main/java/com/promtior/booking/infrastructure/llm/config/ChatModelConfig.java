package com.promtior.booking.infrastructure.llm.config;

import com.promtior.booking.infrastructure.llm.failover.FailoverChatModel;
import com.promtior.booking.infrastructure.llm.failover.FailoverStreamingChatModel;
import com.promtior.booking.infrastructure.llm.tracing.TracingChatModelListener;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Un {@link ChatModel} y un {@link StreamingChatModel} por perfil de Spring: {@code gemini}
 * (primario, con failover automático a Groq ante error transitorio), {@code groq} (Groq forzado) u
 * {@code ollama} (desarrollo offline). Cambiar de proveedor es cambiar el perfil activo -- ver ADR
 * 0001 y ADR 0009. El bean de streaming existe en paralelo al síncrono (no lo reemplaza): {@code
 * BookingAssistant} usa uno u otro según el método que invoque el controller.
 *
 * <p>Cada bean se arma con {@link TracingChatModelListener} (E07.4, ADR 0011) para que el proveedor
 * de respaldo de {@link FailoverChatModel}/{@link FailoverStreamingChatModel} también quede
 * instrumentado -- el listener se registra en el modelo real, no en el wrapper de failover.
 */
@Configuration
@EnableConfigurationProperties({LlmProperties.class, LangfuseProperties.class})
public class ChatModelConfig {

  @Bean
  @Profile("gemini")
  ChatModel geminiChatModel(LlmProperties properties, TracingChatModelListener tracing) {
    return new FailoverChatModel(
        buildGemini(properties.gemini(), tracing), buildGroq(properties.groq(), tracing));
  }

  @Bean
  @Profile("groq")
  ChatModel groqChatModel(LlmProperties properties, TracingChatModelListener tracing) {
    return buildGroq(properties.groq(), tracing);
  }

  @Bean
  @Profile("ollama")
  ChatModel ollamaChatModel(LlmProperties properties, TracingChatModelListener tracing) {
    LlmProperties.Ollama ollama = properties.ollama();
    return OllamaChatModel.builder()
        .baseUrl(ollama.baseUrl())
        .modelName(ollama.modelName())
        .listeners(List.of(tracing))
        .build();
  }

  @Bean
  @Profile("gemini")
  StreamingChatModel geminiStreamingChatModel(
      LlmProperties properties, TracingChatModelListener tracing) {
    return new FailoverStreamingChatModel(
        buildGeminiStreaming(properties.gemini(), tracing),
        buildGroqStreaming(properties.groq(), tracing));
  }

  @Bean
  @Profile("groq")
  StreamingChatModel groqStreamingChatModel(
      LlmProperties properties, TracingChatModelListener tracing) {
    return buildGroqStreaming(properties.groq(), tracing);
  }

  @Bean
  @Profile("ollama")
  StreamingChatModel ollamaStreamingChatModel(
      LlmProperties properties, TracingChatModelListener tracing) {
    LlmProperties.Ollama ollama = properties.ollama();
    return OllamaStreamingChatModel.builder()
        .baseUrl(ollama.baseUrl())
        .modelName(ollama.modelName())
        .listeners(List.of(tracing))
        .build();
  }

  private static ChatModel buildGemini(
      LlmProperties.Gemini gemini, TracingChatModelListener tracing) {
    return GoogleAiGeminiChatModel.builder()
        .apiKey(gemini.apiKey())
        .modelName(gemini.modelName())
        .maxRetries(gemini.maxRetries())
        .listeners(List.of(tracing))
        .build();
  }

  private static ChatModel buildGroq(LlmProperties.Groq groq, TracingChatModelListener tracing) {
    return OpenAiChatModel.builder()
        .baseUrl(groq.baseUrl())
        .apiKey(groq.apiKey())
        .modelName(groq.modelName())
        .listeners(List.of(tracing))
        .build();
  }

  private static StreamingChatModel buildGeminiStreaming(
      LlmProperties.Gemini gemini, TracingChatModelListener tracing) {
    return GoogleAiGeminiStreamingChatModel.builder()
        .apiKey(gemini.apiKey())
        .modelName(gemini.modelName())
        .listeners(List.of(tracing))
        .build();
  }

  private static StreamingChatModel buildGroqStreaming(
      LlmProperties.Groq groq, TracingChatModelListener tracing) {
    return OpenAiStreamingChatModel.builder()
        .baseUrl(groq.baseUrl())
        .apiKey(groq.apiKey())
        .modelName(groq.modelName())
        .listeners(List.of(tracing))
        .build();
  }
}
