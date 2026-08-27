package com.promtior.booking.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Rango de {@link TimeSlot} contiguos, sin huecos, que compone la reserva de una sala. */
public record BookingRange(List<TimeSlot> slots) {

  public BookingRange {
    Objects.requireNonNull(slots, "slots");
    if (slots.isEmpty()) {
      throw new IllegalArgumentException("BookingRange requiere al menos un slot");
    }
    slots = List.copyOf(slots);
    for (int i = 1; i < slots.size(); i++) {
      TimeSlot previous = slots.get(i - 1);
      TimeSlot current = slots.get(i);
      if (!current.start().equals(previous.end())) {
        throw new IllegalArgumentException(
            "BookingRange requiere slots contiguos: hueco entre %s y %s"
                .formatted(previous.start(), current.start()));
      }
    }
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
}
