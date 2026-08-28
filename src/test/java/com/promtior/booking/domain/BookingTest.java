package com.promtior.booking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    BookingErrorException exception =
        assertThrows(BookingErrorException.class, () -> new Booking("", 3, OWNER, Room.C, RANGE));
    assertEquals(new BookingError.MissingTitle(), exception.error());
  }

  @Test
  void rechazaTituloSoloEspacios() {
    assertThrows(BookingErrorException.class, () -> new Booking("   ", 3, OWNER, Room.C, RANGE));
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
        BookingErrorException.class, () -> new Booking("Retro de equipo", 0, OWNER, Room.C, RANGE));
  }

  @Test
  void rechazaAsistentesQueSuperanLaCapacidadDeLaSala() {
    BookingErrorException exception =
        assertThrows(
            BookingErrorException.class,
            () -> new Booking("Retro de equipo", 9, OWNER, Room.C, RANGE));
    assertEquals(new BookingError.CapacityExceeded(Room.C, 9), exception.error());
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

  private static BookingRange rangeFrom(
      int startHour, int startMinute, int endHour, int endMinute) {
    return BookingRange.between(
        new TimeSlot(LocalDateTime.of(2026, 8, 27, startHour, startMinute)),
        new TimeSlot(LocalDateTime.of(2026, 8, 27, endHour, endMinute)));
  }

  /**
   * RN-07, ejemplo textual del enunciado: una cita de 10:00 a 11:30 bloquea todo inicio anterior a
   * 11:30 en la misma sala, y permite uno que empiece exactamente a las 11:30.
   */
  @Test
  void unaReservaDeDiezAOnceYMediaRechazaCualquierInicioAnteriorAOnceYMediaEnLaMismaSala() {
    Booking existente = new Booking("Retro de equipo", 3, OWNER, Room.C, rangeFrom(10, 0, 11, 0));

    Booking empiezaAntesDeQueTermine =
        new Booking("Otra reunión", 3, OWNER, Room.C, rangeFrom(11, 0, 11, 30));
    Booking empiezaExactamenteCuandoTermina =
        new Booking("Otra reunión", 3, OWNER, Room.C, rangeFrom(11, 30, 12, 0));

    assertTrue(existente.overlapsWith(empiezaAntesDeQueTermine));
    assertFalse(existente.overlapsWith(empiezaExactamenteCuandoTermina));
  }

  @Test
  void reservasEnSalasDistintasNuncaSeSolapanAunqueElHorarioCoincida() {
    Booking enSalaC = new Booking("Retro de equipo", 3, OWNER, Room.C, rangeFrom(10, 0, 11, 0));
    Booking enSalaDMismoHorario =
        new Booking("Otra reunión", 3, OWNER, Room.D, rangeFrom(10, 0, 11, 0));

    assertFalse(enSalaC.overlapsWith(enSalaDMismoHorario));
  }

  @Test
  void overlapsWithRechazaOtraReservaNula() {
    Booking booking = new Booking("Retro de equipo", 3, OWNER, Room.C, RANGE);
    assertThrows(NullPointerException.class, () -> booking.overlapsWith(null));
  }

  private static Clock fixedClockAt(LocalDateTime dateTime) {
    return Clock.fixed(dateTime.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
  }

  @Test
  void createAceptaUnInicioFuturoSegunElReloj() {
    Clock clock = fixedClockAt(LocalDateTime.of(2026, 8, 27, 9, 0));
    Booking booking = Booking.create("Retro de equipo", 3, OWNER, Room.C, RANGE, clock);
    assertEquals("Retro de equipo", booking.title());
  }

  @Test
  void createRechazaUnInicioQueYaPaso() {
    Clock clock = fixedClockAt(LocalDateTime.of(2026, 8, 27, 10, 30));
    BookingErrorException exception =
        assertThrows(
            BookingErrorException.class,
            () -> Booking.create("Retro de equipo", 3, OWNER, Room.C, RANGE, clock));
    assertEquals(
        new BookingError.InThePast(
            LocalDateTime.of(2026, 8, 27, 10, 0), LocalDateTime.of(2026, 8, 27, 10, 30)),
        exception.error());
  }

  @Test
  void createRechazaUnRelojNulo() {
    assertThrows(
        NullPointerException.class,
        () -> Booking.create("Retro de equipo", 3, OWNER, Room.C, RANGE, null));
  }
}
