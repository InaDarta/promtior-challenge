package com.promtior.booking.infrastructure.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Prueba el criterio de aceptación de E05.3: dos usuarios tienen conversaciones independientes, y
 * el historial de un mismo usuario le llega al modelo en cada turno, sin dejarlo crecer sin límite.
 */
class BookingAssistantConfigTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(BookingAssistantConfig.class);

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
