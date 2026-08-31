package com.promtior.booking.infrastructure.llm.dto;

import com.promtior.booking.application.IdentifiedBooking;
import com.promtior.booking.domain.Room;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Una reserva propia, sin el {@code owner} del dominio -- redundante para el modelo, que ya sabe
 * que son "mis" reservas -- y con sus horarios expresados como {@code start}/{@code end} de
 * calendario en vez de slots.
 */
public record BookingSummary(
    UUID id, String title, int attendeeCount, Room room, LocalDateTime start, LocalDateTime end) {

  public static BookingSummary from(IdentifiedBooking identifiedBooking) {
    var booking = identifiedBooking.booking();
    return new BookingSummary(
        identifiedBooking.id(),
        booking.title(),
        booking.attendeeCount(),
        booking.room(),
        booking.range().start().start(),
        booking.range().end().end());
  }
}
