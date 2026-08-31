package com.promtior.booking.infrastructure.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code app.llm.*}: credenciales y modelo de cada proveedor. Ver {@code application.yml}; las keys
 * llegan por variable de entorno, nunca versionadas.
 */
@ConfigurationProperties(prefix = "app.llm")
record LlmProperties(Gemini gemini, Groq groq, Ollama ollama) {

  record Gemini(String apiKey, String modelName, int maxRetries) {}

  record Groq(String apiKey, String modelName, String baseUrl) {}

  record Ollama(String baseUrl, String modelName) {}
}
