package com.promtior.booking.infrastructure.rest.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.promtior.booking.domain.BookingError;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.TimeSlot;
import com.promtior.booking.infrastructure.rest.dto.TimeSlotResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Criterio de aceptación de E04.4: cada subtipo de {@link BookingError} tiene su código estable y
 * lleva los datos de su causa, no solo el mensaje.
 */
class BookingProblemsTest {

  private static final TimeSlot SLOT_1000 = new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 0));
  private static final TimeSlot SLOT_1030 = new TimeSlot(LocalDateTime.of(2026, 8, 27, 10, 30));

  @Test
  void capacityExceededEs400ConSuCodigoYLaCapacidadRealYLoPedido() {
    BookingError error = new BookingError.CapacityExceeded(Room.C, 9);

    ProblemDetail problem = BookingProblems.from(error);

    assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
    Map<String, Object> data = problem.getProperties();
    assertEquals("ROOM_CAPACITY_EXCEEDED", data.get("code"));
    assertEquals(Room.C, data.get("room"));
    assertEquals(8, data.get("roomCapacity"));
    assertEquals(9, data.get("requestedAttendees"));
    assertEquals(error.message(), problem.getDetail());
  }

  @Test
  void slotOccupiedEs409ConSuCodigoYLosSlotsEnConflicto() {
    BookingError error = new BookingError.SlotOccupied(Room.B, List.of(SLOT_1000, SLOT_1030));

    ProblemDetail problem = BookingProblems.from(error);

    assertEquals(HttpStatus.CONFLICT.value(), problem.getStatus());
    Map<String, Object> data = problem.getProperties();
    assertEquals("SLOT_TAKEN", data.get("code"));
    assertEquals(Room.B, data.get("room"));
    assertEquals(
        List.of(TimeSlotResponse.from(SLOT_1000), TimeSlotResponse.from(SLOT_1030)),
        data.get("conflictingSlots"));
  }

  @Test
  void maxDurationExceededEs400ConSuCodigoYLoPedidoYElLimite() {
    BookingError error = new BookingError.MaxDurationExceeded(8, 6);

    ProblemDetail problem = BookingProblems.from(error);

    assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
    Map<String, Object> data = problem.getProperties();
    assertEquals("MAX_DURATION_EXCEEDED", data.get("code"));
    assertEquals(8, data.get("requestedSlotCount"));
    assertEquals(6, data.get("maxSlotCount"));
  }

  @Test
  void nonContiguousRangeEs400ConSuCodigoYLosSlotsAAmbosLadosDelHueco() {
    BookingError error = new BookingError.NonContiguousRange(SLOT_1000, SLOT_1030);

    ProblemDetail problem = BookingProblems.from(error);

    assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
    Map<String, Object> data = problem.getProperties();
    assertEquals("NON_CONTIGUOUS_RANGE", data.get("code"));
    assertEquals(TimeSlotResponse.from(SLOT_1000), data.get("before"));
    assertEquals(TimeSlotResponse.from(SLOT_1030), data.get("after"));
  }

  @Test
  void outsideOfficeHoursEs400ConSuCodigoYElRangoPedido() {
    LocalDateTime start = LocalDateTime.of(2026, 8, 27, 20, 0);
    LocalDateTime end = LocalDateTime.of(2026, 8, 27, 20, 30);
    BookingError error = new BookingError.OutsideOfficeHours(start, end);

    ProblemDetail problem = BookingProblems.from(error);

    assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
    Map<String, Object> data = problem.getProperties();
    assertEquals("OUTSIDE_OFFICE_HOURS", data.get("code"));
    assertEquals(start, data.get("start"));
    assertEquals(end, data.get("end"));
  }

  @Test
  void missingTitleEs400ConSuCodigoYSinDatosAdicionales() {
    BookingError error = new BookingError.MissingTitle();

    ProblemDetail problem = BookingProblems.from(error);

    assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
    Map<String, Object> data = problem.getProperties();
    assertEquals("TITLE_REQUIRED", data.get("code"));
    assertEquals(Map.of("code", "TITLE_REQUIRED"), data);
  }

  @Test
  void inThePastEs400ConSuCodigoYElInicioPedidoYLaHoraActual() {
    LocalDateTime start = LocalDateTime.of(2026, 8, 27, 9, 0);
    LocalDateTime now = LocalDateTime.of(2026, 8, 27, 9, 30);
    BookingError error = new BookingError.InThePast(start, now);

    ProblemDetail problem = BookingProblems.from(error);

    assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
    Map<String, Object> data = problem.getProperties();
    assertEquals("IN_THE_PAST", data.get("code"));
    assertEquals(start, data.get("start"));
    assertEquals(now, data.get("now"));
  }

  /**
   * Ejercita cada subtipo a través del switch de {@link BookingProblems#from}: no necesita
   * actualizarse si se agrega un subtipo, el compilador obliga a cubrirlo.
   */
  @Test
  void todosLosSubtiposProducenUnCodigoDistinto() {
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

    List<Object> codigos =
        errores.stream().map(e -> BookingProblems.from(e).getProperties().get("code")).toList();

    assertEquals(7, Set.copyOf(codigos).size());
    assertFalse(codigos.contains(null));
  }
}
