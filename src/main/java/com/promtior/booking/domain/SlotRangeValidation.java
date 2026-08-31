package com.promtior.booking.domain;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Validaciones de horario de oficina y contigüidad compartidas por los dos rangos de {@link
 * TimeSlot} del dominio: {@link BookingRange} (duración de una reserva, con el límite adicional de
 * RN-05) y {@link QueryRange} (rango de una consulta de disponibilidad, sin ese límite).
 */
final class SlotRangeValidation {

  private static final DayOfWeek FIRST_OFFICE_DAY = DayOfWeek.MONDAY;
  private static final DayOfWeek LAST_OFFICE_DAY = DayOfWeek.FRIDAY;
  private static final LocalTime OFFICE_OPENING = LocalTime.of(8, 0);
  private static final LocalTime OFFICE_CLOSING = LocalTime.of(20, 0);

  private SlotRangeValidation() {}

  static void requireContiguous(List<TimeSlot> slots) {
    for (int i = 1; i < slots.size(); i++) {
      TimeSlot previous = slots.get(i - 1);
      TimeSlot current = slots.get(i);
      if (!current.start().equals(previous.end())) {
        throw new BookingErrorException(new BookingError.NonContiguousRange(previous, current));
      }
    }
  }

  static void requireOfficeHours(List<TimeSlot> slots) {
    LocalDateTime start = slots.get(0).start();
    LocalDateTime end = slots.get(slots.size() - 1).end();
    DayOfWeek day = start.getDayOfWeek();
    if (day.compareTo(FIRST_OFFICE_DAY) < 0 || day.compareTo(LAST_OFFICE_DAY) > 0) {
      throw new BookingErrorException(new BookingError.OutsideOfficeHours(start, end));
    }
    if (start.toLocalTime().isBefore(OFFICE_OPENING)
        || !end.toLocalDate().equals(start.toLocalDate())
        || end.toLocalTime().isAfter(OFFICE_CLOSING)) {
      throw new BookingErrorException(new BookingError.OutsideOfficeHours(start, end));
    }
  }
}
