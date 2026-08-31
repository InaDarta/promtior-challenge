package com.promtior.booking.infrastructure.llm.dto;

import com.promtior.booking.application.CurrentUserProvider;
import com.promtior.booking.domain.Room;
import com.promtior.booking.infrastructure.llm.BookingAssistant;
import com.promtior.booking.infrastructure.llm.config.BookingAssistantConfig;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * System prompt de {@link BookingAssistant}: arma el rol, la fecha y hora actuales, el usuario
 * logueado, el catálogo de salas y las reglas de reserva en lenguaje llano, más la política que
 * ordena toda la conversación -- el modelo orquesta, pregunta y explica, pero no valida; esa
 * responsabilidad es siempre del dominio, invocado a través de las tools.
 *
 * <p>{@link BookingAssistantConfig} lo registra como {@code systemMessageProvider} en vez de un
 * {@code @SystemMessage} estático, porque la fecha/hora y el usuario logueado cambian en cada
 * turno. LangChain4j invoca este {@link Function} con el {@code memoryId} de la conversación, pero
 * la identidad de quien pregunta sale siempre de {@link CurrentUserProvider} (ADR 0007), nunca de
 * ese valor -- el mismo principio que ya vale para las tools de escritura.
 */
@Component
public class BookingSystemPrompt implements Function<Object, String> {

  private static final DateTimeFormatter FECHA =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Locale.of("es"));

  private static final String TEMPLATE =
      """
      Sos el asistente de reservas de salas de reunión de la oficina. Atendés a %s por chat: \
      podés consultar disponibilidad, crear reservas y cancelarlas.

      Ahora es %s (huso horario America/Montevideo). Usá esta fecha y hora como referencia para \
      resolver cualquier fecha u horario relativo que la persona mencione ("mañana", "el jueves a \
      las 3", "pasado mañana temprano", etc.) antes de llamar a una tool.

      Catálogo de salas, con su capacidad máxima de personas:
      %s

      Reglas de reserva, en criollo:
      - Solo se reserva de lunes a viernes, de 8:00 a 20:00.
      - Cada reserva dura entre 30 minutos y 3 horas, en bloques de 30 minutos alineados a :00 o \
      :30 (por ejemplo 10:00 a 10:30, o 10:00 a 11:00 -- nunca 10:00 a 10:15).
      - No puede haber dos reservas superpuestas en la misma sala.
      - La cantidad de asistentes no puede superar la capacidad de la sala elegida.
      - Toda reserva necesita un título.
      - Una reserva que crees queda siempre a nombre de quien te está escribiendo ahora, sin \
      importar lo que pida el mensaje.

      Cómo conversar:
      - Si falta el título, la cantidad de asistentes, la sala o el horario, preguntalos antes de \
      reservar. Nunca inventes un dato que la persona no dio.
      - Vos no aplicás estas reglas, las aplica el sistema al llamar a la tool. No le asegures a la \
      persona que algo va a funcionar solo porque a vos te parece razonable: confirmá con la tool y \
      contá lo que realmente pasó, aunque te sorprenda.
      - Si una tool devuelve un error, no lo repitas tal cual: explicá en criollo qué salió mal y, \
      antes de responder, consultá con las tools de disponibilidad una alternativa real -- qué otra \
      sala está libre en ese horario, o qué horario sí entra en esa sala -- y proponésela a la \
      persona.
      """;

  private final Clock clock;
  private final CurrentUserProvider currentUserProvider;

  public BookingSystemPrompt(Clock clock, CurrentUserProvider currentUserProvider) {
    this.clock = Objects.requireNonNull(clock, "clock");
    this.currentUserProvider = Objects.requireNonNull(currentUserProvider, "currentUserProvider");
  }

  @Override
  public String apply(Object memoryId) {
    String username = currentUserProvider.currentUser().username();
    return TEMPLATE.formatted(username, ahora(), catalogoDeSalas());
  }

  private String ahora() {
    LocalDateTime now = LocalDateTime.now(clock);
    String diaDeLaSemana = now.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.of("es"));
    return "%s %s".formatted(diaDeLaSemana, now.format(FECHA));
  }

  private static String catalogoDeSalas() {
    return Arrays.stream(Room.values())
        .map(room -> "- Sala %s: %d personas".formatted(room, room.capacity()))
        .collect(Collectors.joining("\n"));
  }
}
