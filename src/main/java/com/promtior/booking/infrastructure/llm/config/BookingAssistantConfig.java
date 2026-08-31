package com.promtior.booking.infrastructure.llm.config;

import com.promtior.booking.infrastructure.llm.BookingAssistant;
import com.promtior.booking.infrastructure.llm.dto.BookingSystemPrompt;
import com.promtior.booking.infrastructure.llm.failover.LlmNotConfiguredException;
import com.promtior.booking.infrastructure.llm.tools.BookingQueryTools;
import com.promtior.booking.infrastructure.llm.tools.BookingTools;
import com.promtior.booking.infrastructure.llm.tools.RoomQueryTools;
import com.promtior.booking.infrastructure.llm.tools.SecurityContextToolExecutor;
import com.promtior.booking.infrastructure.llm.tracing.ConversationTraceRegistry;
import com.promtior.booking.infrastructure.llm.tracing.TracingBookingAssistant;
import com.promtior.booking.infrastructure.llm.tracing.TracingToolExecutor;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import io.opentelemetry.api.trace.Tracer;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
 * <p>Las tools de consulta de E05.4 ({@link RoomQueryTools}, {@link BookingQueryTools}), las de
 * escritura de E05.5 ({@link BookingTools}) y el {@link BookingSystemPrompt} de E05.6 se inyectan
 * como parámetro obligatorio, igual que cualquier otro bean de Spring: a diferencia del {@code
 * ChatModel}, no hay ningún escenario en el que no existan en producción, así que un test que arma
 * este bean directamente (sin el resto del contexto de Spring) tiene que proveerlos explícitamente.
 *
 * <p>Las tools se registran vía {@link #tracedTools} en vez de {@code .tools(Object...)}: arma el
 * mismo {@code Map<ToolSpecification, ToolExecutor>} que LangChain4j construiría solo, pero
 * envolviendo cada {@link ToolExecutor} con {@link TracingToolExecutor} (E07.4, ADR 0011) sin tocar
 * las clases de tools. El {@link BookingAssistant} resultante se envuelve además con {@link
 * TracingBookingAssistant} para el span raíz de cada turno.
 */
@Configuration
@EnableConfigurationProperties(LangfuseProperties.class)
public class BookingAssistantConfig {

  /**
   * Ventana chica a propósito: alcanza para que "cancelala" en el segundo turno resuelva a lo
   * charlado en el primero, sin dejar crecer sin límite los tokens (y el costo) de cada turno.
   */
  private static final int MAX_MESSAGES_EN_MEMORIA = 20;

  @Bean
  BookingAssistant bookingAssistant(
      ObjectProvider<ChatModel> chatModelProvider,
      ObjectProvider<StreamingChatModel> streamingChatModelProvider,
      BookingSystemPrompt bookingSystemPrompt,
      RoomQueryTools roomQueryTools,
      BookingQueryTools bookingQueryTools,
      BookingTools bookingTools,
      Tracer tracer,
      LangfuseProperties langfuseProperties,
      ConversationTraceRegistry traceRegistry) {
    Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();
    tools.putAll(tracedTools(roomQueryTools, tracer, langfuseProperties, traceRegistry));
    tools.putAll(tracedTools(bookingQueryTools, tracer, langfuseProperties, traceRegistry));
    tools.putAll(tracedTools(bookingTools, tracer, langfuseProperties, traceRegistry));

    BookingAssistant assistant =
        AiServices.builder(BookingAssistant.class)
            .chatModel(deferredChatModel(chatModelProvider))
            .streamingChatModel(deferredStreamingChatModel(streamingChatModelProvider))
            .chatMemoryProvider(
                memoryId -> MessageWindowChatMemory.withMaxMessages(MAX_MESSAGES_EN_MEMORIA))
            .systemMessageProvider(bookingSystemPrompt)
            .tools(tools)
            .build();
    return new TracingBookingAssistant(assistant, tracer, langfuseProperties, traceRegistry);
  }

  private static Map<ToolSpecification, ToolExecutor> tracedTools(
      Object toolObject,
      Tracer tracer,
      LangfuseProperties langfuseProperties,
      ConversationTraceRegistry traceRegistry) {
    Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();
    for (ToolSpecification specification : ToolSpecifications.toolSpecificationsFrom(toolObject)) {
      Method method = toolMethod(toolObject, specification.name());
      ToolExecutor executor =
          new SecurityContextToolExecutor(new DefaultToolExecutor(toolObject, method));
      tools.put(
          specification,
          new TracingToolExecutor(
              executor, specification.name(), tracer, langfuseProperties, traceRegistry));
    }
    return tools;
  }

  private static Method toolMethod(Object toolObject, String toolName) {
    for (Method method : toolObject.getClass().getDeclaredMethods()) {
      if (method.isAnnotationPresent(Tool.class) && method.getName().equals(toolName)) {
        return method;
      }
    }
    throw new IllegalStateException(
        "No se encontró el método @Tool %s en %s".formatted(toolName, toolObject.getClass()));
  }

  private static ChatModel deferredChatModel(ObjectProvider<ChatModel> chatModelProvider) {
    return new ChatModel() {
      @Override
      public ChatResponse chat(ChatRequest request) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
          throw new LlmNotConfiguredException();
        }
        return chatModel.chat(request);
      }
    };
  }

  /**
   * Análogo de {@link #deferredChatModel} para el camino de streaming: sin proveedor configurado,
   * el fallo no puede lanzarse hacia arriba -- para cuando este método corre no hay nadie síncrono
   * escuchando esa excepción -- así que se entrega vía {@link
   * StreamingChatResponseHandler#onError}, que es donde el framework espera ver los fallos de un
   * {@link StreamingChatModel}.
   */
  private static StreamingChatModel deferredStreamingChatModel(
      ObjectProvider<StreamingChatModel> streamingChatModelProvider) {
    return new StreamingChatModel() {
      @Override
      public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
        StreamingChatModel streamingChatModel = streamingChatModelProvider.getIfAvailable();
        if (streamingChatModel == null) {
          handler.onError(new LlmNotConfiguredException());
          return;
        }
        streamingChatModel.chat(request, handler);
      }
    };
  }
}
