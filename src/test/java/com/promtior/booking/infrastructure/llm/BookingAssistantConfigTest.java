package com.promtior.booking.infrastructure.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.promtior.booking.application.CancelBooking;
import com.promtior.booking.application.CreateBooking;
import com.promtior.booking.application.GetRoomSchedule;
import com.promtior.booking.application.ListAvailableRooms;
import com.promtior.booking.application.ListMyBookings;
import com.promtior.booking.domain.User;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Prueba el criterio de aceptación de E05.3: dos usuarios tienen conversaciones independientes, y
 * el historial de un mismo usuario le llega al modelo en cada turno, sin dejarlo crecer sin límite.
 * Las tools (E05.4/E05.5) se arman con dobles vacíos: ninguno de estos tests ejercita tool calling
 * real.
 */
class BookingAssistantConfigTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(BookingAssistantConfig.class)
          .withBean(
              ListAvailableRooms.class,
              () -> new ListAvailableRooms(new InMemoryBookingRepository()))
          .withBean(
              GetRoomSchedule.class, () -> new GetRoomSchedule(new InMemoryBookingRepository()))
          .withBean(
              ListMyBookings.class,
              () ->
                  new ListMyBookings(
                      new InMemoryBookingRepository(),
                      new FakeCurrentUserProvider(new User("nadie"))))
          .withBean(
              BookingSystemPrompt.class,
              () ->
                  new BookingSystemPrompt(
                      Clock.systemDefaultZone(), new FakeCurrentUserProvider(new User("nadie"))))
          .withBean(RoomQueryTools.class)
          .withBean(BookingQueryTools.class)
          .withBean(
              CreateBooking.class,
              () ->
                  new CreateBooking(
                      new InMemoryBookingRepository(),
                      new FakeCurrentUserProvider(new User("nadie")),
                      Clock.systemDefaultZone()))
          .withBean(
              CancelBooking.class,
              () ->
                  new CancelBooking(
                      new InMemoryBookingRepository(),
                      new FakeCurrentUserProvider(new User("nadie"))))
          .withBean(BookingTools.class);

  @Test
  void sinUnChatModelEnElContextoElAsistenteExisteYFallaAlConversar() {
    contextRunner.run(
        context -> {
          BookingAssistant assistant = context.getBean(BookingAssistant.class);
          assertThrows(RuntimeException.class, () -> assistant.chat("user1", "hola"));
        });
  }

  @Test
  void dosMemoryIdsDistintosNuncaComparanHistorial() {
    contextRunner
        .withBean(ChatModel.class, EchoHistoryChatModel::new)
        .run(
            context -> {
              BookingAssistant assistant = context.getBean(BookingAssistant.class);
              assistant.chat("user1", "Reservame la sala A mañana a las 10");
              String replyUser2 = assistant.chat("user2", "cancelala");

              assertFalse(replyUser2.contains("sala A"));
            });
  }

  @Test
  void elMismoMemoryIdMantieneElHistorialEntreTurnos() {
    contextRunner
        .withBean(ChatModel.class, EchoHistoryChatModel::new)
        .run(
            context -> {
              BookingAssistant assistant = context.getBean(BookingAssistant.class);
              assistant.chat("user1", "Reservame la sala A mañana a las 10");
              String reply = assistant.chat("user1", "cancelala");

              assertTrue(reply.contains("sala A"));
              assertTrue(reply.contains("cancelala"));
            });
  }

  @Test
  void cadaTurnoLlevaElSystemPromptConElUsuarioLogueadoYElCatalogoDeSalas() {
    AtomicReference<String> systemMessageRecibido = new AtomicReference<>();
    contextRunner
        .withBean(
            ChatModel.class,
            () ->
                new StubChatModel(
                    request -> {
                      request.messages().stream()
                          .filter(SystemMessage.class::isInstance)
                          .map(m -> ((SystemMessage) m).text())
                          .findFirst()
                          .ifPresent(systemMessageRecibido::set);
                      return ChatResponse.builder().aiMessage(AiMessage.from("ok")).build();
                    }))
        .run(
            context -> {
              BookingAssistant assistant = context.getBean(BookingAssistant.class);
              assistant.chat("user1", "hola");

              assertTrue(systemMessageRecibido.get().contains("nadie"));
              assertTrue(systemMessageRecibido.get().contains("Sala A: 4 personas"));
            });
  }

  @Test
  void laVentanaDeMemoriaEsAcotadaYDescartaLosTurnosMasViejos() {
    contextRunner
        .withBean(ChatModel.class, EchoHistoryChatModel::new)
        .run(
            context -> {
              BookingAssistant assistant = context.getBean(BookingAssistant.class);
              String ultimaRespuesta = "";
              for (int turno = 1; turno <= 30; turno++) {
                ultimaRespuesta = assistant.chat("user1", "turno-" + turno);
              }

              assertFalse(ultimaRespuesta.contains("turno-1 "));
              assertTrue(ultimaRespuesta.contains("turno-29"));
            });
  }
}
