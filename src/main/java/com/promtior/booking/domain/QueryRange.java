package com.promtior.booking.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Rango de {@link TimeSlot} contiguos para una <em>consulta</em> de disponibilidad (agenda de una
 * sala, salas libres en un rango) -- a diferencia de {@link BookingRange}, no tiene el límite de
 * duración de RN-05: ese límite rige cuánto puede durar una reserva, no cuánto se puede consultar
 * de una sola vez.
 */
public record QueryRange(List<TimeSlot> slots) {

  public QueryRange {
    Objects.requireNonNull(slots, "slots");
    if (slots.isEmpty()) {
      throw new IllegalArgumentException("QueryRange requiere al menos un slot");
    }
    slots = List.copyOf(slots);
    SlotRangeValidation.requireContiguous(slots);
    SlotRangeValidation.requireOfficeHours(slots);
  }

  /** Construye el rango contiguo de slots entre {@code start} y {@code end}, ambos incluidos. */
  public static QueryRange between(TimeSlot start, TimeSlot end) {
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
    return new QueryRange(range);
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
}
