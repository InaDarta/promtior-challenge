package com.promtior.booking.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/** Bloque de 30 minutos alineado a `:00` o `:30`, identificado por su instante de inicio. */
public record TimeSlot(LocalDateTime start) {

  public TimeSlot {
    Objects.requireNonNull(start, "start");
    if (start.getSecond() != 0 || start.getNano() != 0) {
      throw new IllegalArgumentException("TimeSlot debe alinearse a un minuto exacto: " + start);
    }
    int minute = start.getMinute();
    if (minute != 0 && minute != 30) {
      throw new IllegalArgumentException(
          "TimeSlot debe alinearse a :00 o :30, no a :%02d".formatted(minute));
    }
  }

  public LocalDateTime end() {
    return start.plusMinutes(30);
  }

  /** El slot siguiente, contiguo a este. */
  public TimeSlot next() {
    return new TimeSlot(end());
  }
}
