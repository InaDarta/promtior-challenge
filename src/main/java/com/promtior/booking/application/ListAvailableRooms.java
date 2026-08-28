package com.promtior.booking.application;

import com.promtior.booking.domain.Availability;
import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.Room;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Salas libres en un rango, filtradas por capacidad mínima si se indica.
 *
 * <p>Una sala está libre si ninguno de sus slots en el rango está ocupado. Se apoya en {@link
 * Availability#of} y no reimplementa la lógica de solapamiento de RN-07.
 */
public class ListAvailableRooms {

  private final BookingRepository bookingRepository;

  public ListAvailableRooms(BookingRepository bookingRepository) {
    this.bookingRepository = Objects.requireNonNull(bookingRepository, "bookingRepository");
  }

  /**
   * @param minCapacity capacidad mínima que debe soportar la sala, o {@code null} para no filtrar
   *     por capacidad
   */
  public List<Room> execute(BookingRange range, Integer minCapacity) {
    Objects.requireNonNull(range, "range");
    return Arrays.stream(Room.values())
        .filter(room -> minCapacity == null || room.capacity() >= minCapacity)
        .filter(room -> isFree(room, range))
        .toList();
  }

  private boolean isFree(Room room, BookingRange range) {
    return Availability.of(room, range, bookingRepository.findByRoom(room))
        .occupiedSlots()
        .isEmpty();
  }
}
