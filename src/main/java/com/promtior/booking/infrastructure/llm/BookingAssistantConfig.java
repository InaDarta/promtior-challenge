package com.promtior.booking.infrastructure.llm;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Arma el proxy de {@link BookingAssistant} sobre el {@link ChatModel} activo, con una ventana de
 * memoria acotada por {@code memoryId} (el username del usuario autenticado, nunca un dato que
 * viaje en el body -- ver {@code ChatController}).
 *
 * <p>El bean existe siempre, incluso sin un perfil de proveedor activo (ver {@link
 * ChatModelConfig}): resuelve el {@code ChatModel} real recién en el primer uso, no al armar el
 * proxy. Atarlo a la existencia del bean con {@code @ConditionalOnBean} obligaría a que {@code
 * ChatModelConfig} se procese antes que esta clase, un orden que Spring no garantiza entre
 * configuraciones separadas; resolverlo en cada llamada evita depender de ese orden. Si no hay
 * proveedor configurado, conversar lanza {@link LlmNotConfiguredException} en vez de fallar el
 * arranque de toda la aplicación -- el resto de la API sigue funcionando igual sin credenciales de
 * LLM.
 *
 * <p>{@link BookingTools} (E05.5) se registra igual vía {@link ObjectProvider}, no como parámetro
 * obligatorio: así un test que arma este bean sin el resto del contexto de Spring (sin {@code
 * CreateBooking}/{@code CancelBooking}) sigue pudiendo instanciar el asistente, solo que sin tools.
 */
@Configuration
class BookingAssistantConfig {

  /**
   * Ventana chica a propósito: alcanza para que "cancelala" en el segundo turno resuelva a lo
   * charlado en el primero, sin dejar crecer sin límite los tokens (y el costo) de cada turno.
   */
  private static final int MAX_MESSAGES_EN_MEMORIA = 20;

  @Bean
  BookingAssistant bookingAssistant(
      ObjectProvider<ChatModel> chatModelProvider,
      ObjectProvider<BookingTools> bookingToolsProvider) {
    AiServices<BookingAssistant> assistant =
        AiServices.builder(BookingAssistant.class)
            .chatModel(deferredChatModel(chatModelProvider))
            .chatMemoryProvider(
                memoryId -> MessageWindowChatMemory.withMaxMessages(MAX_MESSAGES_EN_MEMORIA));
    BookingTools bookingTools = bookingToolsProvider.getIfAvailable();
    if (bookingTools != null) {
      assistant.tools(bookingTools);
    }
    return assistant.build();
  }

  private static ChatModel deferredChatModel(ObjectProvider<ChatModel> chatModelProvider) {
    return new ChatModel() {
      @Override
      public ChatResponse doChat(ChatRequest request) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
          throw new LlmNotConfiguredException();
        }
        return chatModel.doChat(request);
      }
    };
  }
}
