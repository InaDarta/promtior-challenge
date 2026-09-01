package com.promtior.booking.domain;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Rango de {@link TimeSlot} contiguos, sin huecos, que compone la reserva de una sala. */
public record BookingRange(List<TimeSlot> slots) {

  /** RN-05: una reserva dura entre 1 y 6 slots, es decir entre 30 minutos y 3 horas. */
  static final int MAX_SLOT_COUNT = 6;

  /** Zona horaria en la que se interpretan los horarios de oficina y del reloj del dominio. */
  public static final ZoneId OFFICE_ZONE = ZoneId.of("America/Montevideo");

  public BookingRange {
    Objects.requireNonNull(slots, "slots");
    if (slots.isEmpty()) {
      throw new IllegalArgumentException("BookingRange requiere al menos un slot");
    }
    slots = List.copyOf(slots);
    if (slots.size() > MAX_SLOT_COUNT) {
      throw new BookingErrorException(
          new BookingError.MaxDurationExceeded(slots.size(), MAX_SLOT_COUNT));
    }
    SlotRangeValidation.requireContiguous(slots);
    SlotRangeValidation.requireOfficeHours(slots);
  }

  /** Construye el rango contiguo de slots entre {@code start} y {@code end}, ambos incluidos. */
  public static BookingRange between(TimeSlot start, TimeSlot end) {
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(end, "end");
    if (end.start().isBefore(start.start())) {
      throw new IllegalArgumentException("end no puede ser anterior a start");
    }
    List<TimeSlot> range = new ArrayList<>();
    for (TimeSlot slot = start; ; slot = slot.next()) {
      range.add(slot);
      if (slot.equals(end)) {
        break;
      }
    }
    return new BookingRange(range);
  }

  public TimeSlot start() {
    return slots.get(0);
  }

  public TimeSlot end() {
    return slots.get(slots.size() - 1);
  }

  public int slotCount() {
    return slots.size();
  }

  /**
   * RN-07: dos rangos se solapan si comparten algún instante. Bordes que se tocan (uno termina
   * cuando el otro empieza) no cuentan como solapamiento.
   */
  public boolean overlaps(BookingRange other) {
    Objects.requireNonNull(other, "other");
    return start().start().isBefore(other.end().end())
        && other.start().start().isBefore(end().end());
  }
}
