package com.promtior.booking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.TimeSlot;
import com.promtior.booking.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListAvailableRoomsTest {

  private static final User OWNER = new User("User1");
  private static final BookingRange RANGE =
      BookingRange.between(
          new TimeSlot(LocalDateTime.of(2026, 8, 31, 10, 0)),
          new TimeSlot(LocalDateTime.of(2026, 8, 31, 10, 30)));

  private final FakeBookingRepository repository = new FakeBookingRepository();
  private final ListAvailableRooms useCase = new ListAvailableRooms(repository);

  @Test
  void sinReservasTodasLasSalasEstanLibres() {
    List<Room> libres = useCase.execute(RANGE, null);

    assertEquals(List.of(Room.values()), libres);
  }

  @Test
  void unaSalaConUnSlotOcupadoEnElRangoQuedaFueraDeLasLibres() {
    repository.save(new Booking("Retro de equipo", 3, OWNER, Room.C, RANGE));

    List<Room> libres = useCase.execute(RANGE, null);

    assertEquals(List.of(Room.A, Room.B, Room.D, Room.E), libres);
  }

  @Test
  void unaReservaQueSoloOcupaParteDelRangoIgualExcluyeLaSala() {
    BookingRange primerSlot =
        BookingRange.between(
            new TimeSlot(LocalDateTime.of(2026, 8, 31, 10, 0)),
            new TimeSlot(LocalDateTime.of(2026, 8, 31, 10, 0)));
    repository.save(new Booking("Retro de equipo", 3, OWNER, Room.C, primerSlot));

    List<Room> libres = useCase.execute(RANGE, null);

    assertEquals(List.of(Room.A, Room.B, Room.D, Room.E), libres);
  }

  @Test
  void unaReservaEnOtraSalaNoAfectaLaDisponibilidad() {
    repository.save(new Booking("Retro de equipo", 3, OWNER, Room.D, RANGE));

    List<Room> libres = useCase.execute(RANGE, null);

    assertEquals(List.of(Room.A, Room.B, Room.C, Room.E), libres);
  }

  @Test
  void filtraPorCapacidadMinimaCuandoSeIndica() {
    List<Room> libres = useCase.execute(RANGE, 10);

    assertEquals(List.of(Room.D, Room.E), libres);
  }
}
