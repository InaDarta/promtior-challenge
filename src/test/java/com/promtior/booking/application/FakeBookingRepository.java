package com.promtior.booking.application;

import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.User;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Doble de {@link BookingRepository} en memoria, para testear casos de uso sin Spring ni base. */
class FakeBookingRepository implements BookingRepository {

  private final Map<UUID, Booking> bookings = new LinkedHashMap<>();

  @Override
  public UUID save(Booking booking) {
    UUID id = UUID.randomUUID();
    bookings.put(id, booking);
    return id;
  }

  @Override
  public List<Booking> findByRoom(Room room) {
    return bookings.values().stream().filter(booking -> booking.room() == room).toList();
  }

  @Override
  public List<IdentifiedBooking> findByOwner(User owner) {
    return bookings.entrySet().stream()
        .filter(entry -> entry.getValue().owner().equals(owner))
        .map(entry -> new IdentifiedBooking(entry.getKey(), entry.getValue()))
        .toList();
  }

  @Override
  public Optional<Booking> findById(UUID id) {
    return Optional.ofNullable(bookings.get(id));
  }

  @Override
  public void deleteById(UUID id) {
    bookings.remove(id);
  }
}
