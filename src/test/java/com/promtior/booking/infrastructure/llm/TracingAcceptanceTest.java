package com.promtior.booking.infrastructure.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promtior.booking.application.CancelBooking;
import com.promtior.booking.application.CreateBooking;
import com.promtior.booking.application.GetRoomSchedule;
import com.promtior.booking.application.ListAvailableRooms;
import com.promtior.booking.application.ListMyBookings;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.User;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Criterio de aceptación de E07.4: una conversación completa queda visible como traza, con sus tool
 * calls anidados. En vez de depender de un proyecto real de Langfuse (no hay credenciales en CI),
 * corre la instrumentación completa contra un {@link SdkTracerProvider} en memoria y verifica la
 * forma del árbol de spans que produce -- span "agent" en la raíz, con las dos llamadas al modelo y
 * la tool call como hijos directos, cada uno con el tipo de observación que Langfuse espera (ver
 * ADR 0011).
 */
class TracingAcceptanceTest {

  private static final User YO = new User("user-yo");
  private static final LocalDateTime INICIO = LocalDateTime.of(2026, 8, 31, 10, 0);
  private static final AttributeKey<String> OBSERVATION_TYPE =
      AttributeKey.stringKey("langfuse.observation.type");
  private static final AttributeKey<String> OBSERVATION_INPUT =
      AttributeKey.stringKey("langfuse.observation.input");

  @Test
  void unTurnoConToolCallQuedaComoUnSpanAgentConLaGeneracionYLaToolComoHijos() {
    InMemorySpanExporter exporter = InMemorySpanExporter.create();
    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build();
    Tracer tracer = tracerProvider.get("test");
    LangfuseProperties enabled = new LangfuseProperties(true);
    TracingChatModelListener chatModelListener =
        new TracingChatModelListener(tracer, enabled, new ObjectMapper());
    InMemoryBookingRepository repository = new InMemoryBookingRepository();
    FakeCurrentUserProvider currentUser = new FakeCurrentUserProvider(YO);

    new ApplicationContextRunner()
        .withUserConfiguration(BookingAssistantConfig.class)
        .withBean(ListAvailableRooms.class, () -> new ListAvailableRooms(repository))
        .withBean(GetRoomSchedule.class, () -> new GetRoomSchedule(repository))
        .withBean(ListMyBookings.class, () -> new ListMyBookings(repository, currentUser))
        .withBean(
            BookingSystemPrompt.class,
            () -> new BookingSystemPrompt(Clock.systemDefaultZone(), currentUser))
        .withBean(RoomQueryTools.class)
        .withBean(BookingQueryTools.class)
        .withBean(
            CreateBooking.class,
            () -> new CreateBooking(repository, currentUser, Clock.systemDefaultZone()))
        .withBean(CancelBooking.class, () -> new CancelBooking(repository, currentUser))
        .withBean(BookingTools.class)
        .withBean(Tracer.class, () -> tracer)
        .withBean(LangfuseProperties.class, () -> enabled)
        .withBean(ConversationTraceRegistry.class, ConversationTraceRegistry::new)
        .withBean(
            ChatModel.class,
            () -> new ListenerInvokingChatModel(stubQueCreaUnaReserva(), chatModelListener))
        .run(
            context -> {
              BookingAssistant assistant = context.getBean(BookingAssistant.class);
              assistant.chat("sesion-1", "reservame la sala C mañana a las 10 para la retro");
            });

    List<SpanData> spans = exporter.getFinishedSpanItems();
    SpanData agentSpan = spanConNombre(spans, "chat");
    List<SpanData> generationSpans = spansConTipo(spans, "generation");
    SpanData toolSpan = spanConTipo(spans, "tool");

    assertEquals(4, spans.size(), "agent + 2 generaciones + 1 tool call");
    assertEquals(2, generationSpans.size());
    assertEquals("agent", agentSpan.getAttributes().get(OBSERVATION_TYPE));
    for (SpanData generationSpan : generationSpans) {
      assertEquals(agentSpan.getSpanId(), generationSpan.getParentSpanId());
    }
    assertEquals(agentSpan.getSpanId(), toolSpan.getParentSpanId());
    assertTrue(toolSpan.getAttributes().get(OBSERVATION_INPUT).contains("Retro de equipo"));
  }

  private static SpanData spanConNombre(List<SpanData> spans, String nombre) {
    return spans.stream()
        .filter(span -> span.getName().equals(nombre))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No se encontró un span llamado " + nombre));
  }

  private static SpanData spanConTipo(List<SpanData> spans, String tipo) {
    return spansConTipo(spans, tipo).stream()
        .findFirst()
        .orElseThrow(() -> new AssertionError("No se encontró un span de tipo " + tipo));
  }

  private static List<SpanData> spansConTipo(List<SpanData> spans, String tipo) {
    return spans.stream()
        .filter(span -> tipo.equals(span.getAttributes().get(OBSERVATION_TYPE)))
        .toList();
  }

  private static ChatModel stubQueCreaUnaReserva() {
    return new StubChatModel(
        request -> {
          Optional<ToolExecutionResultMessage> resultadoDeLaTool =
              request.messages().stream()
                  .filter(ToolExecutionResultMessage.class::isInstance)
                  .map(ToolExecutionResultMessage.class::cast)
                  .findFirst();
          if (resultadoDeLaTool.isPresent()) {
            return ChatResponse.builder().aiMessage(AiMessage.from("listo")).build();
          }
          ToolExecutionRequest toolCall =
              ToolExecutionRequest.builder()
                  .id("call-1")
                  .name("createBooking")
                  .arguments(
                      "{\"title\":\"Retro de equipo\",\"attendeeCount\":3,\"room\":\"%s\",\"start\":\"%s\",\"end\":\"%s\"}"
                          .formatted(Room.C, INICIO, INICIO.plusMinutes(30)))
                  .build();
          return ChatResponse.builder().aiMessage(AiMessage.from(toolCall)).build();
        });
  }
}
