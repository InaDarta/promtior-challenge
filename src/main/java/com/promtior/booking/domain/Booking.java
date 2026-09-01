package com.promtior.booking.domain;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/** Reserva de una sala: título, cantidad de asistentes, propietario, sala y rango horario. */
public record Booking(String title, int attendeeCount, User owner, Room room, BookingRange range) {

  static final int MAX_TITLE_LENGTH = 120;

  public Booking {
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(room, "room");
    Objects.requireNonNull(range, "range");
    if (title.isBlank()) {
      throw new BookingErrorException(new BookingError.MissingTitle());
    }
    if (title.length() > MAX_TITLE_LENGTH) {
      throw new IllegalArgumentException(
          "title no puede superar los %d caracteres, pero tiene %d"
              .formatted(MAX_TITLE_LENGTH, title.length()));
    }
    if (attendeeCount < 1 || attendeeCount > room.capacity()) {
      throw new BookingErrorException(new BookingError.CapacityExceeded(room, attendeeCount));
    }
  }

  /** RN-07: dos reservas en la misma sala no pueden solaparse. */
  public boolean overlapsWith(Booking other) {
    Objects.requireNonNull(other, "other");
    return room == other.room && range.overlaps(other.range);
  }

  /**
   * Crea una reserva rechazando además un inicio que ya haya pasado según {@code clock}, el reloj
   * inyectado para poder testear con una fecha y hora fijas.
   */
  public static Booking create(
      String title, int attendeeCount, User owner, Room room, BookingRange range, Clock clock) {
    Objects.requireNonNull(range, "range");
    Objects.requireNonNull(clock, "clock");
    LocalDateTime now = LocalDateTime.now(clock);
    if (range.start().start().isBefore(now)) {
      throw new BookingErrorException(new BookingError.InThePast(range.start().start(), now));
    }
    return new Booking(title, attendeeCount, owner, room, range);
  }
}
