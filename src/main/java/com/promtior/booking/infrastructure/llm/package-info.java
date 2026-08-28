/**
 * Un {@link dev.langchain4j.model.chat.ChatModel} por perfil de Spring.
 *
 * <p>{@link com.promtior.booking.infrastructure.llm.ChatModelConfig} arma el bean según el perfil
 * activo (gemini, groq u ollama); nada fuera de este paquete menciona un proveedor concreto. {@link
 * com.promtior.booking.infrastructure.llm.FailoverChatModel} es el detalle del perfil {@code
 * gemini}: ante un error transitorio del proveedor primario, delega en el de respaldo en la misma
 * llamada, en vez de exigir un cambio de perfil manual. Ver ADR 0001 y ADR 0009.
 */
package com.promtior.booking.infrastructure.llm;
