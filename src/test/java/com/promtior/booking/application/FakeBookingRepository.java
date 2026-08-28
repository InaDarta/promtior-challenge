package com.promtior.booking.application;

import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.User;
import java.util.ArrayList;
import java.util.List;

/** Doble de {@link BookingRepository} en memoria, para testear casos de uso sin Spring ni base. */
class FakeBookingRepository implements BookingRepository {

  private final List<Booking> bookings = new ArrayList<>();

  @Override
  public void save(Booking booking) {
    bookings.add(booking);
  }

  @Override
  public List<Booking> findByRoom(Room room) {
    return bookings.stream().filter(booking -> booking.room() == room).toList();
  }

  @Override
  public List<Booking> findByOwner(User owner) {
    return bookings.stream().filter(booking -> booking.owner().equals(owner)).toList();
  }
}
