/**
 * Un {@link dev.langchain4j.model.chat.ChatModel} por perfil de Spring, y el asistente de
 * LangChain4j que lo consume.
 *
 * <p>{@link com.promtior.booking.infrastructure.llm.ChatModelConfig} arma el bean según el perfil
 * activo (gemini, groq u ollama); nada fuera de este paquete menciona un proveedor concreto. {@link
 * com.promtior.booking.infrastructure.llm.FailoverChatModel} es el detalle del perfil {@code
 * gemini}: ante un error transitorio del proveedor primario, delega en el de respaldo en la misma
 * llamada, en vez de exigir un cambio de perfil manual. Ver ADR 0001 y ADR 0009.
 *
 * <p>{@link com.promtior.booking.infrastructure.llm.BookingAssistant} es el {@code AiService} que
 * atiende la conversación; {@link com.promtior.booking.infrastructure.llm.BookingAssistantConfig}
 * arma su proxy sobre el {@code ChatModel} activo, con una ventana de memoria acotada por sesión.
 * Solo existe si hay un {@code ChatModel} en el contexto -- ver E05.3.
 *
 * <p>{@link com.promtior.booking.infrastructure.llm.RoomQueryTools} y {@link
 * com.promtior.booking.infrastructure.llm.BookingQueryTools} son las tools de consulta de E05.4:
 * adaptadores finos sobre los casos de uso de {@code application}, sin lógica propia. {@link
 * com.promtior.booking.infrastructure.llm.BookingTools} son las tools de escritura de E05.5:
 * adaptadores finos sobre {@code CreateBooking}/{@code CancelBooking} que nunca reciben un usuario
 * como parámetro (ADR 0007) y traducen una violación de regla a un resultado estructurado en vez de
 * dejar escapar la excepción de dominio.
 *
 * <p>{@link com.promtior.booking.infrastructure.llm.TracingBookingAssistant}, {@link
 * com.promtior.booking.infrastructure.llm.TracingChatModelListener} y {@link
 * com.promtior.booking.infrastructure.llm.TracingToolExecutor} son la instrumentación de E07.4:
 * spans de Langfuse por conversación, llamada al modelo y tool call, apagados por default (ver
 * {@link com.promtior.booking.infrastructure.llm.LangfuseProperties}). Ver ADR 0011.
 */
package com.promtior.booking.infrastructure.llm;
