package com.promtior.booking.infrastructure.llm.dto;

import com.promtior.booking.infrastructure.llm.tools.BookingTools;
import java.util.UUID;

/**
 * Resultado estructurado de {@link BookingTools#createBooking}, para que el modelo lo lea sin
 * necesitar interpretar una excepción: éxito con el id creado, o el {@link
 * com.promtior.booking.domain.BookingError#code() código estable} y el mensaje de la regla violada.
 * Ninguna reserva queda creada cuando {@code success} es {@code false}.
 */
public record CreateBookingResult(
    boolean success, UUID bookingId, String errorCode, String errorMessage) {

  public static CreateBookingResult ok(UUID bookingId) {
    return new CreateBookingResult(true, bookingId, null, null);
  }

  public static CreateBookingResult error(String errorCode, String errorMessage) {
    return new CreateBookingResult(false, null, errorCode, errorMessage);
  }
}
