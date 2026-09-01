package com.promtior.booking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TimeSlotTest {

  @Test
  void aceptaInicioAlineadoEnPunto() {
    LocalDateTime inicio = LocalDateTime.of(2026, 8, 27, 10, 0);
    assertEquals(inicio, new TimeSlot(inicio).start());
  }

  @Test
  void aceptaInicioAlineadoAYMedia() {
    LocalDateTime inicio = LocalDateTime.of(2026, 8, 27, 10, 30);
    assertEquals(inicio, new TimeSlot(inicio).start());
  }

  @Test
  void rechazaInicioNoAlineado() {
    LocalDateTime inicio = LocalDateTime.of(2026, 8, 27, 10, 17);
    assertThrows(IllegalArgumentException.class, () -> new TimeSlot(inicio));
  }

  @Test
  void rechazaInicioConSegundos() {
    LocalDateTime inicio = LocalDateTime.of(2026, 8, 27, 10, 0, 1);
    assertThrows(IllegalArgumentException.class, () -> new TimeSlot(inicio));
  }

  @Test
  void rechazaInicioNulo() {
    assertThrows(NullPointerException.class, () -> new TimeSlot(null));
  }

  @Test
  void endEsTreintaMinutosDespuesDelInicio() {
    TimeSlot slot = new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 0));
    assertEquals(LocalDateTime.of(2026, 8, 27, 10, 30), slot.end());
  }

  @Test
  void nextEsElSlotContiguo() {
    TimeSlot slot = new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 0));
    assertEquals(new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 30)), slot.next());
  }
}
