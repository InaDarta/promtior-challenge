package com.promtior.booking.infrastructure.llm;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

/**
 * {@code AiService} de LangChain4j que atiende la conversación de reservas. {@link
 * BookingAssistantConfig} arma la implementación (un proxy dinámico) sobre el {@link
 * dev.langchain4j.model.chat.ChatModel} activo, con una {@link
 * dev.langchain4j.memory.chat.ChatMemoryProvider} que aísla el historial por {@code memoryId}.
 *
 * <p>Sin tools ni system prompt todavía -- eso es E05.4/E05.5 (tools) y E05.6 (prompt); acá solo se
 * conecta el modelo con la memoria de conversación y el endpoint HTTP.
 */
public interface BookingAssistant {

  String chat(@MemoryId String memoryId, @UserMessage String message);
}
