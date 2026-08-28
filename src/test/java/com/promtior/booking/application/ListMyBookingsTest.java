package com.promtior.booking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.TimeSlot;
import com.promtior.booking.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListMyBookingsTest {

  private static final User YO = new User("User1");
  private static final User OTRO = new User("User2");
  private static final BookingRange RANGE =
      BookingRange.between(
          new TimeSlot(LocalDateTime.of(2026, 8, 31, 10, 0)),
          new TimeSlot(LocalDateTime.of(2026, 8, 31, 10, 30)));

  private final FakeBookingRepository repository = new FakeBookingRepository();

  @Test
  void devuelveSoloLasReservasDelUsuarioAutenticado() {
    Booking miReserva = new Booking("Retro de equipo", 3, YO, Room.C, RANGE);
    repository.save(miReserva);
    repository.save(new Booking("1:1", 2, OTRO, Room.D, RANGE));
    ListMyBookings useCase = new ListMyBookings(repository, new FakeCurrentUserProvider(YO));

    List<Booking> misReservas = useCase.execute();

    assertEquals(List.of(miReserva), misReservas);
  }

  @Test
  void sinReservasPropiasDevuelveListaVacia() {
    repository.save(new Booking("1:1", 2, OTRO, Room.D, RANGE));
    ListMyBookings useCase = new ListMyBookings(repository, new FakeCurrentUserProvider(YO));

    assertTrue(useCase.execute().isEmpty());
  }
}
