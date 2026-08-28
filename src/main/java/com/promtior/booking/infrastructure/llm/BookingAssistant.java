package com.promtior.booking.infrastructure.llm;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * {@code AiService} de LangChain4j que atiende la conversación de reservas. {@link
 * BookingAssistantConfig} arma la implementación (un proxy dinámico) sobre el {@link
 * dev.langchain4j.model.chat.ChatModel} (para {@link #chat}) y el {@link
 * dev.langchain4j.model.chat.StreamingChatModel} (para {@link #chatStream}) activos, con una {@link
 * dev.langchain4j.memory.chat.ChatMemoryProvider} que aísla el historial por {@code memoryId} y es
 * compartida por ambos métodos.
 *
 * <p>{@link BookingAssistantConfig} le agrega las tools de consulta de E05.4 ({@link
 * RoomQueryTools}, {@link BookingQueryTools}) y las de escritura de E05.5 ({@link BookingTools});
 * sin system prompt todavía, eso es E05.6.
 */
public interface BookingAssistant {

  String chat(@MemoryId String memoryId, @UserMessage String message);

  TokenStream chatStream(@MemoryId String memoryId, @UserMessage String message);
}
