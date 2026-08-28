package com.promtior.booking.infrastructure.rest;

import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.TimeSlot;
import java.time.LocalDateTime;

/**
 * Traduce el {@code start}/{@code end} de calendario que recibe la API -- el fin real de la
 * reserva, no el inicio de su último slot -- al {@link BookingRange} que espera el dominio. Ver
 * {@code BookingJpaEntity.toDomain}, que hace la misma conversión al leer de la base.
 */
final class BookingRanges {

  private BookingRanges() {}

  static BookingRange of(LocalDateTime start, LocalDateTime end) {
    return BookingRange.between(new TimeSlot(start), new TimeSlot(end.minusMinutes(30)));
  }
}
