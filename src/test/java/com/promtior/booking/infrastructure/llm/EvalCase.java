package com.promtior.booking.infrastructure.llm;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Un caso de la suite de evaluación en vivo de E07.3: una conversación de uno o más turnos contra
 * un {@link BookingAssistant} con un {@link dev.langchain4j.model.chat.ChatModel} real, con la tool
 * que se espera que dispare el último turno y el criterio, en criollo, con el que {@link
 * BookingAgentEvalRunner} documenta si acertó.
 *
 * <p>{@code toolsEsperadas} vacía significa que el turno final no debería disparar ninguna tool (el
 * modelo debería preguntar un dato que falta, o explicar por qué no puede hacer lo que se le pide)
 * en vez de inventar un argumento o una tool que no existe.
 */
record EvalCase(
    String id,
    String categoria,
    List<Function<EvalContext, String>> turnos,
    List<String> toolsEsperadas,
    String criterioDeAcierto,
    BiConsumer<InMemoryBookingRepository, EvalContext> setup) {

  EvalCase {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(categoria, "categoria");
    Objects.requireNonNull(turnos, "turnos");
    Objects.requireNonNull(toolsEsperadas, "toolsEsperadas");
    Objects.requireNonNull(criterioDeAcierto, "criterioDeAcierto");
    Objects.requireNonNull(setup, "setup");
    if (turnos.isEmpty()) {
      throw new IllegalArgumentException("un EvalCase necesita al menos un turno");
    }
  }

  /** Caso de un solo turno con frase fija, sin reservas previas que sembrar. */
  static EvalCase deUnTurno(
      String id,
      String categoria,
      String frase,
      List<String> toolsEsperadas,
      String criterioDeAcierto) {
    return new EvalCase(
        id,
        categoria,
        List.of(ctx -> frase),
        toolsEsperadas,
        criterioDeAcierto,
        (repository, ctx) -> {});
  }
}
