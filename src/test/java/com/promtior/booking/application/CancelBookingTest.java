package com.promtior.booking.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.TimeSlot;
import com.promtior.booking.domain.User;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CancelBookingTest {

  private static final User YO = new User("User1");
  private static final User OTRO = new User("User2");
  private static final BookingRange RANGE =
      BookingRange.between(
          new TimeSlot(LocalDateTime.of(2026, 8, 31, 10, 0)),
          new TimeSlot(LocalDateTime.of(2026, 8, 31, 10, 30)));

  private final FakeBookingRepository repository = new FakeBookingRepository();

  @Test
  void elDuenioPuedeCancelarSuPropiaReserva() {
    UUID id = repository.save(new Booking("Retro", 3, YO, Room.C, RANGE));
    CancelBooking cancelBooking = new CancelBooking(repository, new FakeCurrentUserProvider(YO));

    cancelBooking.execute(id);

    assertTrue(repository.findById(id).isEmpty());
  }

  @Test
  void cancelarUnaReservaAjenaLanzaBookingNotOwnedExceptionYNoLaElimina() {
    UUID id = repository.save(new Booking("Retro", 3, YO, Room.C, RANGE));
    CancelBooking cancelBooking = new CancelBooking(repository, new FakeCurrentUserProvider(OTRO));

    assertThrows(BookingNotOwnedException.class, () -> cancelBooking.execute(id));
    assertTrue(repository.findById(id).isPresent());
  }

  @Test
  void cancelarUnaReservaInexistenteLanzaLaMismaBookingNotOwnedException() {
    CancelBooking cancelBooking = new CancelBooking(repository, new FakeCurrentUserProvider(YO));

    assertThrows(BookingNotOwnedException.class, () -> cancelBooking.execute(UUID.randomUUID()));
  }
}
