package com.promtior.booking.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Disponibilidad de una sala en un rango: qué slots están libres y cuáles ocupados. */
public record Availability(Room room, List<TimeSlot> freeSlots, List<TimeSlot> occupiedSlots) {

  public Availability {
    Objects.requireNonNull(room, "room");
    Objects.requireNonNull(freeSlots, "freeSlots");
    Objects.requireNonNull(occupiedSlots, "occupiedSlots");
    freeSlots = List.copyOf(freeSlots);
    occupiedSlots = List.copyOf(occupiedSlots);
  }

  /**
   * Calcula la disponibilidad de {@code room} en {@code range}, contra las reservas existentes que
   * ocupan esa sala. Reservas en otras salas no afectan el resultado.
   */
  public static Availability of(Room room, BookingRange range, List<Booking> existingBookings) {
    Objects.requireNonNull(room, "room");
    Objects.requireNonNull(range, "range");
    Objects.requireNonNull(existingBookings, "existingBookings");

    Set<TimeSlot> occupied =
        existingBookings.stream()
            .filter(booking -> booking.room() == room)
            .flatMap(booking -> booking.range().slots().stream())
            .collect(Collectors.toUnmodifiableSet());

    List<TimeSlot> free = new ArrayList<>();
    List<TimeSlot> busy = new ArrayList<>();
    for (TimeSlot slot : range.slots()) {
      if (occupied.contains(slot)) {
        busy.add(slot);
      } else {
        free.add(slot);
      }
    }
    return new Availability(room, free, busy);
  }

  /** RN-07 expresado como error: los slots ocupados del rango, si los hay. */
  public Optional<BookingError.SlotOccupied> conflict() {
    return occupiedSlots.isEmpty()
        ? Optional.empty()
        : Optional.of(new BookingError.SlotOccupied(room, occupiedSlots));
  }
}
