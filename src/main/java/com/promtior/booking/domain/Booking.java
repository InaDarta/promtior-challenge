package com.promtior.booking.domain;

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
      throw new IllegalArgumentException("title no puede estar vacío ni contener solo espacios");
    }
    if (title.length() > MAX_TITLE_LENGTH) {
      throw new IllegalArgumentException(
          "title no puede superar los %d caracteres, pero tiene %d"
              .formatted(MAX_TITLE_LENGTH, title.length()));
    }
    if (attendeeCount < 1 || attendeeCount > room.capacity()) {
      throw new IllegalArgumentException(
          "attendeeCount debe estar entre 1 y la capacidad de la sala %s (%d), pero fue %d"
              .formatted(room, room.capacity(), attendeeCount));
    }
  }
}
