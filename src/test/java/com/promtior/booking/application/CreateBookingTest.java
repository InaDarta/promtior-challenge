package com.promtior.booking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.BookingErrorException;
import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.TimeSlot;
import com.promtior.booking.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateBookingTest {

  private static final User YO = new User("User1");

  /** Lunes, dentro de horario de oficina; el {@link Clock} fijo del test está en 1970. */
  private static final BookingRange RANGE_FUTURA =
      BookingRange.between(
          new TimeSlot(LocalDateTime.of(2026, 8, 31, 10, 0)),
          new TimeSlot(LocalDateTime.of(2026, 8, 31, 10, 30)));

  private final FakeBookingRepository repository = new FakeBookingRepository();
  private final CreateBooking createBooking =
      new CreateBooking(
          repository,
          new FakeCurrentUserProvider(YO),
          Clock.fixed(Instant.EPOCH, ZoneId.of("UTC")));

  @Test
  void creaLaReservaConElOwnerDelCurrentUserProviderYDevuelveElIdConElQueSePuedeEncontrarla() {
    UUID id = createBooking.execute("Retro de equipo", 3, Room.C, RANGE_FUTURA);

    Booking esperada = new Booking("Retro de equipo", 3, YO, Room.C, RANGE_FUTURA);
    assertEquals(Optional.of(esperada), repository.findById(id));
  }

  @Test
  void unaViolacionDeUnaReglaDeDominioNuncaLlegaAPersistir() {
    assertThrows(
        BookingErrorException.class, () -> createBooking.execute("", 3, Room.C, RANGE_FUTURA));

    assertTrue(repository.findByOwner(YO).isEmpty());
  }
}
