package com.promtior.booking.infrastructure.llm;

import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.QueryRange;
import com.promtior.booking.domain.TimeSlot;
import java.time.LocalDateTime;

/**
 * Traduce el {@code start}/{@code end} de calendario que recibe una tool -- el fin real del rango,
 * no el inicio de su último slot -- al rango que espera el dominio: {@link BookingRange} al crear
 * una reserva (con el límite de duración de RN-05), {@link QueryRange} al consultar disponibilidad
 * (sin ese límite). Misma conversión que {@code BookingRanges} en {@code infrastructure.rest},
 * duplicada a propósito: cada adaptador es autónomo y no depende de los DTOs internos de otro.
 */
final class BookingRanges {

  private BookingRanges() {}

  static BookingRange of(LocalDateTime start, LocalDateTime end) {
    return BookingRange.between(new TimeSlot(start), new TimeSlot(end.minusMinutes(30)));
  }

  static QueryRange query(LocalDateTime start, LocalDateTime end) {
    return QueryRange.between(new TimeSlot(start), new TimeSlot(end.minusMinutes(30)));
  }
}
