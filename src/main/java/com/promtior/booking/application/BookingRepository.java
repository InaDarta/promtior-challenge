package com.promtior.booking.application;

import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.Room;
import java.util.List;

/**
 * Puerto de persistencia de {@link Booking}, implementado por un adaptador en {@code
 * infrastructure}.
 */
public interface BookingRepository {

  void save(Booking booking);

  /** Las reservas existentes en {@code room}, insumo del cálculo de disponibilidad del dominio. */
  List<Booking> findByRoom(Room room);
}
