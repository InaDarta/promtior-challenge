package com.promtior.booking.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Violación de una regla de reserva. Cada subtipo lleva los datos de su causa (capacidad real,
 * slots en conflicto, límite superado), no solo un mensaje, para que quien lo reciba pueda explicar
 * exactamente qué pasó.
 */
public sealed interface BookingError {

  /** RN-03: la cantidad de asistentes pedida supera la capacidad de {@code room}. */
  record CapacityExceeded(Room room, int requestedAttendees) implements BookingError {
    public CapacityExceeded {
      Objects.requireNonNull(room, "room");
    }
  }

  /** RN-07: uno o más slots del rango pedido ya están ocupados en {@code room}. */
  record SlotOccupied(Room room, List<TimeSlot> conflictingSlots) implements BookingError {
    public SlotOccupied {
      Objects.requireNonNull(room, "room");
      Objects.requireNonNull(conflictingSlots, "conflictingSlots");
      conflictingSlots = List.copyOf(conflictingSlots);
      if (conflictingSlots.isEmpty()) {
        throw new IllegalArgumentException("conflictingSlots no puede estar vacío");
      }
    }
  }

  /** RN-05: el rango pedido supera el máximo de slots permitido. */
  record MaxDurationExceeded(int requestedSlotCount, int maxSlotCount) implements BookingError {}

  /** El rango pedido tiene un hueco entre dos slots que deberían ser contiguos. */
  record NonContiguousRange(TimeSlot before, TimeSlot after) implements BookingError {
    public NonContiguousRange {
      Objects.requireNonNull(before, "before");
      Objects.requireNonNull(after, "after");
    }
  }

  /** El rango pedido cae fuera del horario de oficina (lun-vie 08:00-20:00). */
  record OutsideOfficeHours(LocalDateTime start, LocalDateTime end) implements BookingError {
    public OutsideOfficeHours {
      Objects.requireNonNull(start, "start");
      Objects.requireNonNull(end, "end");
    }
  }

  /** RN-08: el título está vacío o contiene solo espacios. */
  record MissingTitle() implements BookingError {}

  /** El inicio pedido ya pasó según el reloj del dominio. */
  record InThePast(LocalDateTime start, LocalDateTime now) implements BookingError {
    public InThePast {
      Objects.requireNonNull(start, "start");
      Objects.requireNonNull(now, "now");
    }
  }

  /** Mensaje legible por humanos. Exhaustivo por tipo de {@link BookingError}, sin rama default. */
  default String message() {
    return switch (this) {
      case CapacityExceeded e ->
          "la sala %s tiene capacidad para %d personas, se pidieron %d"
              .formatted(e.room(), e.room().capacity(), e.requestedAttendees());
      case SlotOccupied e ->
          "la sala %s está ocupada de %s a %s"
              .formatted(
                  e.room(),
                  e.conflictingSlots().get(0).start(),
                  e.conflictingSlots().get(e.conflictingSlots().size() - 1).end());
      case MaxDurationExceeded e ->
          "el rango pedido tiene %d slots, el máximo permitido es %d"
              .formatted(e.requestedSlotCount(), e.maxSlotCount());
      case NonContiguousRange e ->
          "hay un hueco entre %s y %s".formatted(e.before().end(), e.after().start());
      case OutsideOfficeHours e ->
          "el rango de %s a %s cae fuera del horario de oficina".formatted(e.start(), e.end());
      case MissingTitle e -> "el título no puede estar vacío ni contener solo espacios";
      case InThePast e -> "el inicio %s ya pasó, ahora es %s".formatted(e.start(), e.now());
    };
  }
}
