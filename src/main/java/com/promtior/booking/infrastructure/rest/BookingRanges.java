package com.promtior.booking.infrastructure.rest;

import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.QueryRange;
import com.promtior.booking.domain.TimeSlot;
import java.time.LocalDateTime;

/**
 * Traduce el {@code start}/{@code end} de calendario que recibe la API -- el fin real del rango, no
 * el inicio de su último slot -- al rango que espera el dominio: {@link BookingRange} al crear una
 * reserva (con el límite de duración de RN-05), {@link QueryRange} al consultar disponibilidad (sin
 * ese límite: se puede pedir la agenda de todo el día). Ver {@code BookingJpaEntity.toDomain}, que
 * hace la misma conversión de {@link BookingRange} al leer de la base.
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
