package com.promtior.booking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookingRangeTest {

  private static TimeSlot slotAt(int hour, int minute) {
    return new TimeSlot(LocalDateTime.of(2026, 8, 27, hour, minute));
  }

  @Test
  void aceptaSlotsContiguos() {
    BookingRange range = new BookingRange(List.of(slotAt(10, 0), slotAt(10, 30), slotAt(11, 0)));
    assertEquals(3, range.slotCount());
    assertEquals(slotAt(10, 0), range.start());
    assertEquals(slotAt(11, 0), range.end());
  }

  @Test
  void rechazaUnHuecoEntreSlots() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new BookingRange(List.of(slotAt(10, 0), slotAt(11, 0))));
  }

  @Test
  void rechazaListaVacia() {
    assertThrows(IllegalArgumentException.class, () -> new BookingRange(List.of()));
  }

  @Test
  void rechazaSlotsNulos() {
    assertThrows(NullPointerException.class, () -> new BookingRange(null));
  }

  @Test
  void betweenConstruyeLosSlotsIntermedios() {
    BookingRange range = BookingRange.between(slotAt(10, 0), slotAt(11, 0));
    assertEquals(List.of(slotAt(10, 0), slotAt(10, 30), slotAt(11, 0)), range.slots());
  }

  @Test
  void betweenConUnSoloSlotProduceUnRangoDeUnSlot() {
    BookingRange range = BookingRange.between(slotAt(10, 0), slotAt(10, 0));
    assertEquals(1, range.slotCount());
  }

  @Test
  void betweenRechazaEndAnteriorAStart() {
    assertThrows(
        IllegalArgumentException.class, () -> BookingRange.between(slotAt(11, 0), slotAt(10, 0)));
  }

  @Test
  void aceptaExactamenteSeisSlots() {
    BookingRange range = BookingRange.between(slotAt(9, 0), slotAt(11, 30));
    assertEquals(6, range.slotCount());
  }

  @Test
  void rechazaMasDeSeisSlotsConErrorQueNombraElLimite() {
    List<TimeSlot> sieteSlots =
        List.of(
            slotAt(9, 0),
            slotAt(9, 30),
            slotAt(10, 0),
            slotAt(10, 30),
            slotAt(11, 0),
            slotAt(11, 30),
            slotAt(12, 0));
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new BookingRange(sieteSlots));
    assertEquals(
        "BookingRange no puede superar los 6 slots (3 horas), pero tiene 7",
        exception.getMessage());
  }

  @Test
  void betweenRechazaTresHorasYMedia() {
    assertThrows(
        IllegalArgumentException.class, () -> BookingRange.between(slotAt(9, 0), slotAt(12, 0)));
  }

  @Test
  void rangosQueSeCruzanSeSolapan() {
    BookingRange diezAOnceYMedia = BookingRange.between(slotAt(10, 0), slotAt(11, 0));
    BookingRange diezYMediaAOnce = BookingRange.between(slotAt(10, 30), slotAt(11, 30));
    assertTrue(diezAOnceYMedia.overlaps(diezYMediaAOnce));
    assertTrue(diezYMediaAOnce.overlaps(diezAOnceYMedia));
  }

  @Test
  void unRangoContenidoEnOtroSeSolapan() {
    BookingRange nueveAOnce = BookingRange.between(slotAt(9, 0), slotAt(10, 30));
    BookingRange diezADiezYMedia = BookingRange.between(slotAt(10, 0), slotAt(10, 0));
    assertTrue(nueveAOnce.overlaps(diezADiezYMedia));
  }

  @Test
  void rangosQueSoloSeTocanEnElBordeNoSeSolapan() {
    BookingRange diezAOnceYMedia = BookingRange.between(slotAt(10, 0), slotAt(11, 0));
    BookingRange onceYMediaADoce = BookingRange.between(slotAt(11, 30), slotAt(12, 0));
    assertFalse(diezAOnceYMedia.overlaps(onceYMediaADoce));
    assertFalse(onceYMediaADoce.overlaps(diezAOnceYMedia));
  }

  @Test
  void rangosDisjuntosNoSeSolapan() {
    BookingRange diezAOnce = BookingRange.between(slotAt(10, 0), slotAt(10, 30));
    BookingRange doceATrece = BookingRange.between(slotAt(12, 0), slotAt(12, 30));
    assertFalse(diezAOnce.overlaps(doceATrece));
  }

  @Test
  void overlapsRechazaOtroNulo() {
    BookingRange diezAOnce = BookingRange.between(slotAt(10, 0), slotAt(10, 30));
    assertThrows(NullPointerException.class, () -> diezAOnce.overlaps(null));
  }

  @Test
  void aceptaElPrimerSlotDelHorarioDeOficina() {
    BookingRange range = new BookingRange(List.of(slotAt(8, 0)));
    assertEquals(slotAt(8, 0), range.start());
  }

  @Test
  void aceptaElUltimoSlotDelHorarioDeOficina() {
    BookingRange range = new BookingRange(List.of(slotAt(19, 30)));
    assertEquals(slotAt(19, 30), range.end());
  }

  @Test
  void rechazaUnInicioAnteriorAlHorarioDeOficina() {
    assertThrows(IllegalArgumentException.class, () -> new BookingRange(List.of(slotAt(7, 30))));
  }

  @Test
  void rechazaUnFinPosteriorAlHorarioDeOficina() {
    assertThrows(IllegalArgumentException.class, () -> new BookingRange(List.of(slotAt(20, 0))));
  }

  @Test
  void rechazaUnSabado() {
    TimeSlot sabado = new TimeSlot(LocalDateTime.of(2026, 8, 29, 10, 0));
    assertThrows(IllegalArgumentException.class, () -> new BookingRange(List.of(sabado)));
  }

  @Test
  void rechazaUnDomingo() {
    TimeSlot domingo = new TimeSlot(LocalDateTime.of(2026, 8, 30, 10, 0));
    assertThrows(IllegalArgumentException.class, () -> new BookingRange(List.of(domingo)));
  }
}
