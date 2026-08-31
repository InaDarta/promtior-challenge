package com.promtior.booking.infrastructure.llm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.promtior.booking.application.ListMyBookings;
import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.TimeSlot;
import com.promtior.booking.domain.User;
import com.promtior.booking.infrastructure.llm.FakeCurrentUserProvider;
import com.promtior.booking.infrastructure.llm.InMemoryBookingRepository;
import com.promtior.booking.infrastructure.llm.dto.BookingSummary;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Prueba el criterio de aceptación de E05.4 para la tool de reservas propias: delega en {@link
 * ListMyBookings} sin lógica propia, y expone el id que necesita cancelar, sin el {@code owner} del
 * dominio.
 */
class BookingQueryToolsTest {

  private static final User YO = new User("User1");
  private static final User OTRO = new User("User2");
  private static final LocalDateTime START = LocalDateTime.of(2026, 8, 31, 10, 0);
  private static final BookingRange RANGE =
      BookingRange.between(new TimeSlot(START), new TimeSlot(START));

  private final InMemoryBookingRepository repository = new InMemoryBookingRepository();

  @Test
  void listMyBookingsDelegaEnElCasoDeUsoYExponeElIdParaCancelar() {
    Booking miReserva = new Booking("Retro de equipo", 3, YO, Room.C, RANGE);
    UUID id = repository.save(miReserva);
    repository.save(new Booking("1:1", 2, OTRO, Room.D, RANGE));
    BookingQueryTools tools =
        new BookingQueryTools(new ListMyBookings(repository, new FakeCurrentUserProvider(YO)));

    List<BookingSummary> misReservas = tools.listMyBookings();

    assertEquals(
        List.of(new BookingSummary(id, "Retro de equipo", 3, Room.C, START, START.plusMinutes(30))),
        misReservas);
  }

  @Test
  void sinReservasPropiasDevuelveListaVacia() {
    repository.save(new Booking("1:1", 2, OTRO, Room.D, RANGE));
    BookingQueryTools tools =
        new BookingQueryTools(new ListMyBookings(repository, new FakeCurrentUserProvider(YO)));

    assertTrue(tools.listMyBookings().isEmpty());
  }
}
