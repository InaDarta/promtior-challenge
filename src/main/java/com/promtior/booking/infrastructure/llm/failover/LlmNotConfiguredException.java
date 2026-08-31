package com.promtior.booking.infrastructure.llm.failover;

import dev.langchain4j.model.chat.ChatModel;

/**
 * No hay un {@link ChatModel} en el contexto: ningún perfil de proveedor ({@code gemini}, {@code
 * groq} u {@code ollama}) está activo. La traducción a una respuesta HTTP (503, en vez de un 500)
 * vive en {@code infrastructure.rest.ChatExceptionHandler}.
 */
public class LlmNotConfiguredException extends RuntimeException {}
