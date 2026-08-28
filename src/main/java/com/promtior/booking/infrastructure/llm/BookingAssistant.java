package com.promtior.booking.infrastructure.llm;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

/**
 * {@code AiService} de LangChain4j que atiende la conversación de reservas. {@link
 * BookingAssistantConfig} arma la implementación (un proxy dinámico) sobre el {@link
 * dev.langchain4j.model.chat.ChatModel} activo, con una {@link
 * dev.langchain4j.memory.chat.ChatMemoryProvider} que aísla el historial por {@code memoryId}.
 *
 * <p>{@link BookingAssistantConfig} le agrega las tools de consulta de E05.4 ({@link
 * RoomQueryTools}, {@link BookingQueryTools}) y las de escritura de E05.5 ({@link BookingTools});
 * sin system prompt todavía, eso es E05.6.
 */
public interface BookingAssistant {

  String chat(@MemoryId String memoryId, @UserMessage String message);
}
