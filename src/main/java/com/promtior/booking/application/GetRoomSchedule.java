package com.promtior.booking.application;

import com.promtior.booking.domain.Availability;
import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.Room;
import java.util.Objects;

/**
 * Slots libres y ocupados de una sala en un rango. Delega el cálculo en {@link Availability#of}.
 */
public class GetRoomSchedule {

  private final BookingRepository bookingRepository;

  public GetRoomSchedule(BookingRepository bookingRepository) {
    this.bookingRepository = Objects.requireNonNull(bookingRepository, "bookingRepository");
  }

  public Availability execute(Room room, BookingRange range) {
    Objects.requireNonNull(room, "room");
    Objects.requireNonNull(range, "range");
    return Availability.of(room, range, bookingRepository.findByRoom(room));
  }
}
