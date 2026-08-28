package com.promtior.booking.infrastructure.llm;

import com.promtior.booking.application.BookingConflictException;
import com.promtior.booking.application.BookingRepository;
import com.promtior.booking.application.IdentifiedBooking;
import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.BookingError;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.User;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Doble de {@link BookingRepository} en memoria, para testear {@link BookingTools} sin Spring ni
 * base. A diferencia del doble homónimo de {@code application}, {@link #save} simula el constraint
 * de exclusión de Postgres (RN-07): dos reservas de la misma sala que se solapan no coexisten.
 */
class FakeBookingRepository implements BookingRepository {

  private final Map<UUID, Booking> bookings = new LinkedHashMap<>();

  @Override
  public UUID save(Booking booking) {
    bookings.values().stream()
        .filter(existing -> existing.overlapsWith(booking))
        .findFirst()
        .ifPresent(
            conflicting -> {
              throw new BookingConflictException(
                  new BookingError.SlotOccupied(
                      booking.room(), List.of(conflicting.range().start())));
            });
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
