package com.promtior.booking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AvailabilityTest {

  private static final User OWNER = new User("user1");

  private static Booking booking(Room room, BookingRange range) {
    return new Booking("Reunión", 2, OWNER, room, range);
  }

  @Test
  void sinReservasExistentesTodoElRangoEstaLibre() {
    QueryRange range =
        QueryRange.between(
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 0)),
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 11, 30)));

    Availability availability = Availability.of(Room.C, range, List.of());

    assertEquals(range.slots(), availability.freeSlots());
    assertTrue(availability.occupiedSlots().isEmpty());
    assertEquals(Optional.empty(), availability.conflict());
  }

  @Test
  void unaReservaParcialEnElMedioDelRangoOcupaSoloEsosSlots() {
    QueryRange rangoConsultado =
        QueryRange.between(
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 0)),
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 11, 30)));
    BookingRange rangoReservado =
        BookingRange.between(
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 30)),
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 30)));
    Booking existente = booking(Room.C, rangoReservado);

    Availability availability = Availability.of(Room.C, rangoConsultado, List.of(existente));

    assertEquals(
        List.of(
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 0)),
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 11, 0)),
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 11, 30))),
        availability.freeSlots());
    assertEquals(
        List.of(new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 30))), availability.occupiedSlots());
  }

  @Test
  void conflictDevuelveUnSlotOccupiedConLosSlotsOcupados() {
    QueryRange rangoConsultado =
        QueryRange.between(
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 0)),
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 30)));
    BookingRange rangoReservado =
        BookingRange.between(
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 0)),
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 30)));
    Booking existente = booking(Room.C, rangoReservado);

    Availability availability = Availability.of(Room.C, rangoConsultado, List.of(existente));

    Optional<BookingError.SlotOccupied> conflicto = availability.conflict();
    assertTrue(conflicto.isPresent());
    assertEquals(Room.C, conflicto.get().room());
    assertEquals(rangoConsultado.slots(), conflicto.get().conflictingSlots());
  }

  @Test
  void reservasEnOtraSalaNoAfectanLaDisponibilidad() {
    QueryRange rangoConsultado =
        QueryRange.between(
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 0)),
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 30)));
    BookingRange rangoReservado =
        BookingRange.between(
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 0)),
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 30)));
    Booking enOtraSala = booking(Room.D, rangoReservado);

    Availability availability = Availability.of(Room.C, rangoConsultado, List.of(enOtraSala));

    assertEquals(rangoConsultado.slots(), availability.freeSlots());
    assertTrue(availability.occupiedSlots().isEmpty());
  }

  @Test
  void unaConsultaDeMasDeTresHorasNoTieneLimiteDeDuracion() {
    QueryRange diaCompleto =
        QueryRange.between(
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 8, 0)),
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 19, 30)));

    Availability availability = Availability.of(Room.C, diaCompleto, List.of());

    assertEquals(24, diaCompleto.slotCount());
    assertEquals(diaCompleto.slots(), availability.freeSlots());
  }

  @Test
  void ofRechazaArgumentosNulos() {
    QueryRange range =
        QueryRange.between(
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 0)),
            new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 30)));
    assertThrows(NullPointerException.class, () -> Availability.of(null, range, List.of()));
    assertThrows(NullPointerException.class, () -> Availability.of(Room.C, null, List.of()));
    assertThrows(NullPointerException.class, () -> Availability.of(Room.C, range, null));
  }
}
