package com.promtior.booking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BookingErrorTest {

  private static final TimeSlot SLOT_1000 = new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 0));
  private static final TimeSlot SLOT_1030 = new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 30));

  @Test
  void capacityExceededRechazaSalaNula() {
    assertThrows(NullPointerException.class, () -> new BookingError.CapacityExceeded(null, 9));
  }

  @Test
  void capacityExceededMensajeIncluyeCapacidadRealYLoPedido() {
    BookingError error = new BookingError.CapacityExceeded(Room.C, 9);
    assertEquals("la sala C tiene capacidad para 8 personas, se pidieron 9", error.message());
  }

  @Test
  void slotOccupiedRechazaListaNula() {
    assertThrows(NullPointerException.class, () -> new BookingError.SlotOccupied(Room.C, null));
  }

  @Test
  void slotOccupiedRechazaListaVacia() {
    assertThrows(
        IllegalArgumentException.class, () -> new BookingError.SlotOccupied(Room.C, List.of()));
  }

  @Test
  void slotOccupiedMensajeIncluyeLaSalaYElRangoOcupado() {
    BookingError error = new BookingError.SlotOccupied(Room.C, List.of(SLOT_1000, SLOT_1030));
    assertEquals("la sala C está ocupada de 2026-08-27T10:00 a 2026-08-27T11:00", error.message());
  }

  @Test
  void maxDurationExceededMensajeIncluyeLoPedidoYElLimite() {
    BookingError error = new BookingError.MaxDurationExceeded(8, 6);
    assertEquals("el rango pedido tiene 8 slots, el máximo permitido es 6", error.message());
  }

  @Test
  void nonContiguousRangeRechazaSlotsNulos() {
    assertThrows(
        NullPointerException.class, () -> new BookingError.NonContiguousRange(null, SLOT_1030));
    assertThrows(
        NullPointerException.class, () -> new BookingError.NonContiguousRange(SLOT_1000, null));
  }

  @Test
  void nonContiguousRangeMensajeIncluyeElHueco() {
    BookingError error = new BookingError.NonContiguousRange(SLOT_1000, SLOT_1030);
    assertEquals("hay un hueco entre 2026-08-27T10:30 y 2026-08-27T10:30", error.message());
  }

  @Test
  void outsideOfficeHoursRechazaFechasNulas() {
    LocalDateTime dateTime = LocalDateTime.of(2026, 8, 27, 21, 0);
    assertThrows(
        NullPointerException.class, () -> new BookingError.OutsideOfficeHours(null, dateTime));
    assertThrows(
        NullPointerException.class, () -> new BookingError.OutsideOfficeHours(dateTime, null));
  }

  @Test
  void outsideOfficeHoursMensajeIncluyeElRangoPedido() {
    LocalDateTime start = LocalDateTime.of(2026, 8, 27, 20, 0);
    LocalDateTime end = LocalDateTime.of(2026, 8, 27, 20, 30);
    BookingError error = new BookingError.OutsideOfficeHours(start, end);
    assertEquals(
        "el rango de 2026-08-27T20:00 a 2026-08-27T20:30 cae fuera del horario de oficina",
        error.message());
  }

  @Test
  void missingTitleMensajeEsFijo() {
    BookingError error = new BookingError.MissingTitle();
    assertEquals("el título no puede estar vacío ni contener solo espacios", error.message());
  }

  @Test
  void inThePastRechazaFechasNulas() {
    LocalDateTime dateTime = LocalDateTime.of(2026, 8, 27, 10, 0);
    assertThrows(NullPointerException.class, () -> new BookingError.InThePast(null, dateTime));
    assertThrows(NullPointerException.class, () -> new BookingError.InThePast(dateTime, null));
  }

  @Test
  void inThePastMensajeIncluyeElInicioPedidoYLaHoraActual() {
    LocalDateTime start = LocalDateTime.of(2026, 8, 27, 9, 0);
    LocalDateTime now = LocalDateTime.of(2026, 8, 27, 9, 30);
    BookingError error = new BookingError.InThePast(start, now);
    assertEquals("el inicio 2026-08-27T09:00 ya pasó, ahora es 2026-08-27T09:30", error.message());
  }

  /**
   * Criterio de aceptación: un switch con pattern matching sobre BookingError es exhaustivo sin
   * rama default. Este test ejercita cada subtipo a través de ese switch (el de {@link
   * BookingError#message()}) y no necesita actualizarse si se agrega un subtipo: el compilador
   * obliga a cubrirlo.
   */
  @Test
  void todosLosSubtiposTienenMensaje() {
    List<BookingError> errores =
        List.of(
            new BookingError.CapacityExceeded(Room.A, 5),
            new BookingError.SlotOccupied(Room.A, List.of(SLOT_1000)),
            new BookingError.MaxDurationExceeded(7, 6),
            new BookingError.NonContiguousRange(SLOT_1000, SLOT_1030),
            new BookingError.OutsideOfficeHours(
                LocalDateTime.of(2026, 8, 27, 21, 0), LocalDateTime.of(2026, 8, 27, 21, 30)),
            new BookingError.MissingTitle(),
            new BookingError.InThePast(
                LocalDateTime.of(2026, 8, 27, 9, 0), LocalDateTime.of(2026, 8, 27, 9, 30)));

    for (BookingError error : errores) {
      assertEquals(false, error.message().isBlank());
    }
  }

  /**
   * Contrato de E04.4: cada subtipo tiene un código estable y distinto, exhaustivo sin rama
   * default, del que dependen tanto {@code BookingProblems} (REST) como {@code BookingTools}
   * (E05.5).
   */
  @Test
  void todosLosSubtiposTienenUnCodigoDistinto() {
    List<BookingError> errores =
        List.of(
            new BookingError.CapacityExceeded(Room.A, 5),
            new BookingError.SlotOccupied(Room.A, List.of(SLOT_1000)),
            new BookingError.MaxDurationExceeded(7, 6),
            new BookingError.NonContiguousRange(SLOT_1000, SLOT_1030),
            new BookingError.OutsideOfficeHours(
                LocalDateTime.of(2026, 8, 27, 21, 0), LocalDateTime.of(2026, 8, 27, 21, 30)),
            new BookingError.MissingTitle(),
            new BookingError.InThePast(
                LocalDateTime.of(2026, 8, 27, 9, 0), LocalDateTime.of(2026, 8, 27, 9, 30)));

    List<String> codigos = errores.stream().map(BookingError::code).toList();

    assertEquals(errores.size(), Set.copyOf(codigos).size());
  }
}
