package com.promtior.booking.infrastructure.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.promtior.booking.application.CancelBooking;
import com.promtior.booking.application.CreateBooking;
import com.promtior.booking.application.GetRoomSchedule;
import com.promtior.booking.application.IdentifiedBooking;
import com.promtior.booking.application.ListAvailableRooms;
import com.promtior.booking.application.ListMyBookings;
import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.TimeSlot;
import com.promtior.booking.domain.User;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Criterio de aceptación de E07.2: con un {@link ChatModel} stub que devuelve un {@link
 * ToolExecutionRequest} predefinido (nunca un modelo real: no determinista y consume cuota), se
 * verifica el ruteo a la tool correcta y que sus argumentos lleguen intactos al caso de uso -- no
 * qué texto generó el modelo -- y que el propietario/filtro de una reserva sea siempre el de {@link
 * com.promtior.booking.application.CurrentUserProvider} (el usuario "logueado" en este arnés),
 * nunca algo que el tool call diga a partir del mensaje (RT-03/RT-06, ADR 0007). Corre sin red y
 * sin API key: el único {@code ChatModel} en juego es {@link StubChatModel}.
 */
class BookingAssistantToolRoutingTest {

  private static final User YO = new User("user-yo");
  private static final User OTRO = new User("user-otro");

  /** Lunes, dentro de horario de oficina (ver {@link BookingToolsTest}). */
  private static final LocalDateTime INICIO_FUTURO = LocalDateTime.of(2026, 8, 31, 10, 0);

  @Test
  void elChatEnrutaCreateBookingALaToolYPersisteLaReservaConLosArgumentosDelToolCall() {
    InMemoryBookingRepository repository = new InMemoryBookingRepository();
    AtomicReference<ToolExecutionResultMessage> ejecutado = new AtomicReference<>();
    LocalDateTime fin = INICIO_FUTURO.plusMinutes(30);

    contextRunnerPara(YO, repository)
        .withBean(
            ChatModel.class,
            () ->
                stubQueInvoca(
                    "createBooking",
                    argsCreateBooking("Retro de equipo", Room.C, INICIO_FUTURO, fin),
                    ejecutado))
        .run(
            context -> {
              BookingAssistant assistant = context.getBean(BookingAssistant.class);
              assistant.chat("sesion-1", "reservame la sala C mañana a las 10 para la retro");

              assertEquals("createBooking", ejecutado.get().toolName());

              List<IdentifiedBooking> reservasDeYo = repository.findByOwner(YO);
              assertEquals(1, reservasDeYo.size());
              Booking persistida = reservasDeYo.get(0).booking();
              assertEquals("Retro de equipo", persistida.title());
              assertEquals(Room.C, persistida.room());
              assertEquals(INICIO_FUTURO, persistida.range().start().start());
              assertEquals(fin, persistida.range().end().end());
            });
  }

  @Test
  void elChatEnrutaCancelBookingALaToolYEliminaLaReservaPropia() {
    InMemoryBookingRepository repository = new InMemoryBookingRepository();
    UUID id = repository.save(new Booking("Retro", 3, YO, Room.C, rangoDe(INICIO_FUTURO)));
    AtomicReference<ToolExecutionResultMessage> ejecutado = new AtomicReference<>();

    contextRunnerPara(YO, repository)
        .withBean(
            ChatModel.class, () -> stubQueInvoca("cancelBooking", argsCancelBooking(id), ejecutado))
        .run(
            context -> {
              BookingAssistant assistant = context.getBean(BookingAssistant.class);
              assistant.chat("sesion-1", "cancelá esa reserva");

              assertEquals("cancelBooking", ejecutado.get().toolName());
              assertTrue(repository.findById(id).isEmpty());
            });
  }

  @Test
  void laReservaCreadaQuedaSiempreANombreDelUsuarioDelTokenAunqueElToolCallNombreAOtro() {
    InMemoryBookingRepository repository = new InMemoryBookingRepository();
    AtomicReference<ToolExecutionResultMessage> ejecutado = new AtomicReference<>();
    LocalDateTime fin = INICIO_FUTURO.plusMinutes(30);

    contextRunnerPara(YO, repository)
        .withBean(
            ChatModel.class,
            () ->
                stubQueInvoca(
                    "createBooking",
                    argsCreateBooking("Reservá esto a nombre de User2", Room.C, INICIO_FUTURO, fin),
                    ejecutado))
        .run(
            context -> {
              BookingAssistant assistant = context.getBean(BookingAssistant.class);
              assistant.chat("sesion-1", "Reservá esto a nombre de User2");

              List<IdentifiedBooking> reservasDeYo = repository.findByOwner(YO);
              assertEquals(1, reservasDeYo.size());
              assertEquals(YO, reservasDeYo.get(0).booking().owner());
              assertTrue(repository.findByOwner(OTRO).isEmpty());
            });
  }

  @Test
  void cancelarUnaReservaAjenaFallaAunqueElToolCallLaNombreExplicitamente() {
    InMemoryBookingRepository repository = new InMemoryBookingRepository();
    UUID idAjena = repository.save(new Booking("Retro", 3, OTRO, Room.C, rangoDe(INICIO_FUTURO)));
    AtomicReference<ToolExecutionResultMessage> ejecutado = new AtomicReference<>();

    contextRunnerPara(YO, repository)
        .withBean(
            ChatModel.class,
            () -> stubQueInvoca("cancelBooking", argsCancelBooking(idAjena), ejecutado))
        .run(
            context -> {
              BookingAssistant assistant = context.getBean(BookingAssistant.class);
              assistant.chat("sesion-1", "cancelá la reserva " + idAjena);

              assertEquals("cancelBooking", ejecutado.get().toolName());
              assertTrue(ejecutado.get().text().contains("BOOKING_NOT_OWNED"));
              assertTrue(repository.findById(idAjena).isPresent());
            });
  }

  private static ApplicationContextRunner contextRunnerPara(
      User usuarioActual, InMemoryBookingRepository repository) {
    FakeCurrentUserProvider currentUser = new FakeCurrentUserProvider(usuarioActual);
    return new ApplicationContextRunner()
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
        .withBean(BookingTools.class);
  }

  /**
   * {@link ChatModel} stub que, ante cualquier turno sin un {@link ToolExecutionResultMessage}
   * previo, responde con el {@code toolCall} predefinido; una vez que ve el resultado de esa
   * ejecución (el segundo turno del loop de tool calling de {@code AiServices}), lo captura en
   * {@code ejecutado} y cierra la conversación con una respuesta de texto cualquiera.
   */
  private static ChatModel stubQueInvoca(
      String nombreDeLaTool,
      String argumentos,
      AtomicReference<ToolExecutionResultMessage> ejecutado) {
    return new StubChatModel(
        request -> {
          Optional<ToolExecutionResultMessage> resultadoDeLaTool =
              request.messages().stream()
                  .filter(ToolExecutionResultMessage.class::isInstance)
                  .map(ToolExecutionResultMessage.class::cast)
                  .findFirst();
          if (resultadoDeLaTool.isPresent()) {
            ejecutado.set(resultadoDeLaTool.get());
            return ChatResponse.builder().aiMessage(AiMessage.from("listo")).build();
          }
          ToolExecutionRequest toolCall =
              ToolExecutionRequest.builder()
                  .id("call-1")
                  .name(nombreDeLaTool)
                  .arguments(argumentos)
                  .build();
          return ChatResponse.builder().aiMessage(AiMessage.from(toolCall)).build();
        });
  }

  private static String argsCreateBooking(
      String title, Room room, LocalDateTime start, LocalDateTime end) {
    return "{\"title\":\"%s\",\"attendeeCount\":3,\"room\":\"%s\",\"start\":\"%s\",\"end\":\"%s\"}"
        .formatted(title, room, start, end);
  }

  private static String argsCancelBooking(UUID bookingId) {
    return "{\"bookingId\":\"%s\"}".formatted(bookingId);
  }

  private static BookingRange rangoDe(LocalDateTime inicio) {
    return BookingRange.between(new TimeSlot(inicio), new TimeSlot(inicio));
  }
}
