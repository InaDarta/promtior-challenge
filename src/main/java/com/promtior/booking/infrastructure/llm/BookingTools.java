package com.promtior.booking.infrastructure.llm;

import com.promtior.booking.application.BookingConflictException;
import com.promtior.booking.application.BookingNotOwnedException;
import com.promtior.booking.application.CancelBooking;
import com.promtior.booking.application.CreateBooking;
import com.promtior.booking.domain.BookingErrorException;
import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.Room;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Tools de escritura que {@link BookingAssistantConfig} registra en {@link BookingAssistant}:
 * adaptadores finos sobre {@link CreateBooking}/{@link CancelBooking}, sin lógica propia.
 *
 * <p>Ninguno de los dos métodos recibe un usuario como parámetro -- el propietario de la reserva
 * creada y el filtro de "solo reservas propias" al cancelar salen siempre de {@link
 * com.promtior.booking.application.CurrentUserProvider}, nunca de un argumento que el modelo
 * complete a partir del texto del chat (RT-03/RT-06, ADR 0007). Una violación de una regla de
 * dominio no se propaga como excepción: se traduce a un resultado estructurado con el código
 * estable de {@link com.promtior.booking.domain.BookingError#code()}, para que el modelo pueda leer
 * la causa y explicarla en la conversación en vez de que esta se corte.
 */
@Component
class BookingTools {

  private final CreateBooking createBookingUseCase;
  private final CancelBooking cancelBookingUseCase;

  BookingTools(CreateBooking createBookingUseCase, CancelBooking cancelBookingUseCase) {
    this.createBookingUseCase =
        Objects.requireNonNull(createBookingUseCase, "createBookingUseCase");
    this.cancelBookingUseCase =
        Objects.requireNonNull(cancelBookingUseCase, "cancelBookingUseCase");
  }

  @Tool(
      "Crea una reserva de sala a nombre del usuario autenticado. La reserva siempre queda a"
          + " nombre de quien está conversando, sin importar lo que pida el mensaje.")
  CreateBookingResult createBooking(
      @P("Título o motivo de la reserva") String title,
      @P("Cantidad de asistentes") int attendeeCount,
      @P("Sala a reservar, una letra entre A y E") Room room,
      @P("Inicio de la reserva, formato ISO-8601 (ej: 2026-08-31T10:00:00)") LocalDateTime start,
      @P("Fin de la reserva, formato ISO-8601 (ej: 2026-08-31T11:00:00)") LocalDateTime end) {
    try {
      BookingRange range = BookingRanges.of(start, end);
      UUID bookingId = createBookingUseCase.execute(title, attendeeCount, room, range);
      return CreateBookingResult.ok(bookingId);
    } catch (BookingErrorException e) {
      return CreateBookingResult.error(e.error().code(), e.getMessage());
    } catch (BookingConflictException e) {
      return CreateBookingResult.error(e.conflict().code(), e.getMessage());
    }
  }

  @Tool(
      "Cancela una reserva propia dado su id. No puede cancelar una reserva de otro usuario ni"
          + " una que no existe.")
  CancelBookingResult cancelBooking(@P("Id de la reserva a cancelar") UUID bookingId) {
    try {
      cancelBookingUseCase.execute(bookingId);
      return CancelBookingResult.ok();
    } catch (BookingNotOwnedException e) {
      return CancelBookingResult.error(
          "BOOKING_NOT_OWNED", "no existe una reserva propia con ese id");
    }
  }
}
