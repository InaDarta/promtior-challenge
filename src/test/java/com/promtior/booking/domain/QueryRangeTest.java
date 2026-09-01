package com.promtior.booking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class QueryRangeTest {

  private static TimeSlot slotAt(int hour, int minute) {
    return new TimeSlot(LocalDateTime.of(2026, 8, 27, hour, minute));
  }

  @Test
  void aceptaSlotsContiguos() {
    QueryRange range = new QueryRange(List.of(slotAt(10, 0), slotAt(10, 30), slotAt(11, 0)));
    assertEquals(3, range.slotCount());
    assertEquals(slotAt(10, 0), range.start());
    assertEquals(slotAt(11, 0), range.end());
  }

  @Test
  void rechazaUnHuecoEntreSlots() {
    BookingErrorException exception =
        assertThrows(
            BookingErrorException.class,
            () -> new QueryRange(List.of(slotAt(10, 0), slotAt(11, 0))));
    assertEquals(
        new BookingError.NonContiguousRange(slotAt(10, 0), slotAt(11, 0)), exception.error());
  }

  @Test
  void rechazaListaVacia() {
    assertThrows(IllegalArgumentException.class, () -> new QueryRange(List.of()));
  }

  @Test
  void rechazaSlotsNulos() {
    assertThrows(NullPointerException.class, () -> new QueryRange(null));
  }

  @Test
  void betweenConstruyeLosSlotsIntermedios() {
    QueryRange range = QueryRange.between(slotAt(10, 0), slotAt(11, 0));
    assertEquals(List.of(slotAt(10, 0), slotAt(10, 30), slotAt(11, 0)), range.slots());
  }

  @Test
  void betweenRechazaEndAnteriorAStart() {
    assertThrows(
        IllegalArgumentException.class, () -> QueryRange.between(slotAt(11, 0), slotAt(10, 0)));
  }

  @Test
  void aDiferenciaDeBookingRangeAceptaMasDeSeisSlots() {
    QueryRange diaCompleto = QueryRange.between(slotAt(8, 0), slotAt(19, 30));
    assertEquals(24, diaCompleto.slotCount());
  }

  @Test
  void aceptaElPrimerSlotDelHorarioDeOficina() {
    QueryRange range = new QueryRange(List.of(slotAt(8, 0)));
    assertEquals(slotAt(8, 0), range.start());
  }

  @Test
  void aceptaElUltimoSlotDelHorarioDeOficina() {
    QueryRange range = new QueryRange(List.of(slotAt(19, 30)));
    assertEquals(slotAt(19, 30), range.end());
  }

  @Test
  void rechazaUnInicioAnteriorAlHorarioDeOficina() {
    assertThrows(BookingErrorException.class, () -> new QueryRange(List.of(slotAt(7, 30))));
  }

  @Test
  void rechazaUnFinPosteriorAlHorarioDeOficina() {
    assertThrows(BookingErrorException.class, () -> new QueryRange(List.of(slotAt(20, 0))));
  }

  @Test
  void rechazaUnSabado() {
    TimeSlot sabado = new TimeSlot(LocalDateTime.of(2026, 8, 29, 10, 0));
    assertThrows(BookingErrorException.class, () -> new QueryRange(List.of(sabado)));
  }

  @Test
  void rechazaUnDomingo() {
    TimeSlot domingo = new TimeSlot(LocalDateTime.of(2026, 8, 30, 10, 0));
    assertThrows(BookingErrorException.class, () -> new QueryRange(List.of(domingo)));
  }
}
