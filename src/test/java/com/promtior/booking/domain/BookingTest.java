package com.promtior.booking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class BookingTest {

  private static final User OWNER = new User("user1");
  private static final BookingRange RANGE =
      BookingRange.between(
          new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 0)),
          new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 30)));

  @Test
  void aceptaUnaReservaValida() {
    Booking booking = new Booking("Retro de equipo", 3, OWNER, Room.C, RANGE);
    assertEquals("Retro de equipo", booking.title());
    assertEquals(3, booking.attendeeCount());
  }

  @Test
  void rechazaTituloNulo() {
    assertThrows(NullPointerException.class, () -> new Booking(null, 3, OWNER, Room.C, RANGE));
  }

  @Test
  void rechazaTituloVacio() {
    assertThrows(IllegalArgumentException.class, () -> new Booking("", 3, OWNER, Room.C, RANGE));
  }

  @Test
  void rechazaTituloSoloEspacios() {
    assertThrows(IllegalArgumentException.class, () -> new Booking("   ", 3, OWNER, Room.C, RANGE));
  }

  @Test
  void rechazaTituloQueSuperaElLargoMaximo() {
    String tituloLargo = "a".repeat(Booking.MAX_TITLE_LENGTH + 1);
    assertThrows(
        IllegalArgumentException.class, () -> new Booking(tituloLargo, 3, OWNER, Room.C, RANGE));
  }

  @Test
  void aceptaTituloEnElLargoMaximo() {
    String tituloLimite = "a".repeat(Booking.MAX_TITLE_LENGTH);
    Booking booking = new Booking(tituloLimite, 3, OWNER, Room.C, RANGE);
    assertEquals(tituloLimite, booking.title());
  }

  @Test
  void rechazaCeroAsistentes() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Booking("Retro de equipo", 0, OWNER, Room.C, RANGE));
  }

  @Test
  void rechazaAsistentesQueSuperanLaCapacidadDeLaSala() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new Booking("Retro de equipo", 9, OWNER, Room.C, RANGE));
    assertEquals(
        "attendeeCount debe estar entre 1 y la capacidad de la sala C (8), pero fue 9",
        exception.getMessage());
  }

  @Test
  void aceptaAsistentesIgualesALaCapacidadDeLaSala() {
    Booking booking = new Booking("Retro de equipo", 8, OWNER, Room.C, RANGE);
    assertEquals(8, booking.attendeeCount());
  }

  @Test
  void rechazaPropietarioNulo() {
    assertThrows(
        NullPointerException.class, () -> new Booking("Retro de equipo", 3, null, Room.C, RANGE));
  }

  @Test
  void rechazaSalaNula() {
    assertThrows(
        NullPointerException.class, () -> new Booking("Retro de equipo", 3, OWNER, null, RANGE));
  }

  @Test
  void rechazaRangoNulo() {
    assertThrows(
        NullPointerException.class, () -> new Booking("Retro de equipo", 3, OWNER, Room.C, null));
  }
}
