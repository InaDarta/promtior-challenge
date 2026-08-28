package com.promtior.booking.infrastructure.llm;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
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
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
class ChatModelConfig {

  @Bean
  @Profile("gemini")
  ChatModel geminiChatModel(LlmProperties properties) {
    return new FailoverChatModel(buildGemini(properties.gemini()), buildGroq(properties.groq()));
  }

  @Bean
  @Profile("groq")
  ChatModel groqChatModel(LlmProperties properties) {
    return buildGroq(properties.groq());
  }

  @Bean
  @Profile("ollama")
  ChatModel ollamaChatModel(LlmProperties properties) {
    LlmProperties.Ollama ollama = properties.ollama();
    return OllamaChatModel.builder()
        .baseUrl(ollama.baseUrl())
        .modelName(ollama.modelName())
        .build();
  }

  @Bean
  @Profile("gemini")
  StreamingChatModel geminiStreamingChatModel(LlmProperties properties) {
    return new FailoverStreamingChatModel(
        buildGeminiStreaming(properties.gemini()), buildGroqStreaming(properties.groq()));
  }

  @Bean
  @Profile("groq")
  StreamingChatModel groqStreamingChatModel(LlmProperties properties) {
    return buildGroqStreaming(properties.groq());
  }

  @Bean
  @Profile("ollama")
  StreamingChatModel ollamaStreamingChatModel(LlmProperties properties) {
    LlmProperties.Ollama ollama = properties.ollama();
    return OllamaStreamingChatModel.builder()
        .baseUrl(ollama.baseUrl())
        .modelName(ollama.modelName())
        .build();
  }

  private static ChatModel buildGemini(LlmProperties.Gemini gemini) {
    return GoogleAiGeminiChatModel.builder()
        .apiKey(gemini.apiKey())
        .modelName(gemini.modelName())
        .maxRetries(gemini.maxRetries())
        .build();
  }

  private static ChatModel buildGroq(LlmProperties.Groq groq) {
    return OpenAiChatModel.builder()
        .baseUrl(groq.baseUrl())
        .apiKey(groq.apiKey())
        .modelName(groq.modelName())
        .build();
  }

  private static StreamingChatModel buildGeminiStreaming(LlmProperties.Gemini gemini) {
    return GoogleAiGeminiStreamingChatModel.builder()
        .apiKey(gemini.apiKey())
        .modelName(gemini.modelName())
        .maxRetries(gemini.maxRetries())
        .build();
  }

  private static StreamingChatModel buildGroqStreaming(LlmProperties.Groq groq) {
    return OpenAiStreamingChatModel.builder()
        .baseUrl(groq.baseUrl())
        .apiKey(groq.apiKey())
        .modelName(groq.modelName())
        .build();
  }
}
