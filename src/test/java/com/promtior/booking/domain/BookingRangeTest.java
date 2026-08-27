package com.promtior.booking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
