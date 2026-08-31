package com.promtior.booking.infrastructure.rest.dto;

import com.promtior.booking.application.IdentifiedBooking;
import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.Room;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingResponse(
    UUID id, String title, int attendeeCount, Room room, LocalDateTime start, LocalDateTime end) {

  public static BookingResponse from(UUID id, Booking booking) {
    return new BookingResponse(
        id,
        booking.title(),
        booking.attendeeCount(),
        booking.room(),
        booking.range().start().start(),
        booking.range().end().end());
  }

  public static BookingResponse from(IdentifiedBooking identifiedBooking) {
    return from(identifiedBooking.id(), identifiedBooking.booking());
  }
}
