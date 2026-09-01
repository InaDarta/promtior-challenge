package com.promtior.booking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RoomTest {

  @Test
  void cadaSalaTieneSuCapacidad() {
    assertEquals(4, Room.A.capacity());
    assertEquals(6, Room.B.capacity());
    assertEquals(8, Room.C.capacity());
    assertEquals(12, Room.D.capacity());
    assertEquals(20, Room.E.capacity());
  }

  @Test
  void elCatalogoTieneExactamenteCincoSalas() {
    assertEquals(5, Room.values().length);
  }
}
