package com.promtior.booking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.promtior.booking.domain.Availability;
import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.QueryRange;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.TimeSlot;
import com.promtior.booking.domain.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class GetRoomScheduleTest {

  private static final User OWNER = new User("User1");
  private static final QueryRange RANGE =
      QueryRange.between(
          new TimeSlot(LocalDateTime.of(2026, 8, 31, 10, 0)),
          new TimeSlot(LocalDateTime.of(2026, 8, 31, 11, 0)));

  private final FakeBookingRepository repository = new FakeBookingRepository();
  private final GetRoomSchedule useCase = new GetRoomSchedule(repository);

  @Test
  void devuelveLosSlotsOcupadosPorUnaReservaExistente() {
    BookingRange reservado =
        BookingRange.between(
            new TimeSlot(LocalDateTime.of(2026, 8, 31, 10, 0)),
            new TimeSlot(LocalDateTime.of(2026, 8, 31, 10, 30)));
    repository.save(new Booking("Retro de equipo", 3, OWNER, Room.C, reservado));

    Availability disponibilidad = useCase.execute(Room.C, RANGE);

    assertEquals(reservado.slots(), disponibilidad.occupiedSlots());
  }

  @Test
  void sinReservasTodoElRangoQuedaLibre() {
    Availability disponibilidad = useCase.execute(Room.C, RANGE);

    assertEquals(RANGE.slots(), disponibilidad.freeSlots());
  }

  @Test
  void noConsideraReservasDeOtraSala() {
    BookingRange reservado =
        BookingRange.between(
            new TimeSlot(LocalDateTime.of(2026, 8, 31, 10, 0)),
            new TimeSlot(LocalDateTime.of(2026, 8, 31, 11, 0)));
    repository.save(new Booking("Retro de equipo", 3, OWNER, Room.D, reservado));

    Availability disponibilidad = useCase.execute(Room.C, RANGE);

    assertEquals(RANGE.slots(), disponibilidad.freeSlots());
  }

  @Test
  void unaConsultaDeAgendaDeTodoElDiaNoTieneLimiteDeDuracion() {
    QueryRange diaCompleto =
        QueryRange.between(
            new TimeSlot(LocalDateTime.of(2026, 8, 31, 8, 0)),
            new TimeSlot(LocalDateTime.of(2026, 8, 31, 19, 30)));

    Availability disponibilidad = useCase.execute(Room.C, diaCompleto);

    assertEquals(diaCompleto.slots(), disponibilidad.freeSlots());
  }
}
