package com.promtior.booking.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Rango de {@link TimeSlot} contiguos, sin huecos, que compone la reserva de una sala. */
public record BookingRange(List<TimeSlot> slots) {

  /** RN-05: una reserva dura entre 1 y 6 slots, es decir entre 30 minutos y 3 horas. */
  static final int MAX_SLOT_COUNT = 6;

  public BookingRange {
    Objects.requireNonNull(slots, "slots");
    if (slots.isEmpty()) {
      throw new IllegalArgumentException("BookingRange requiere al menos un slot");
    }
    slots = List.copyOf(slots);
    if (slots.size() > MAX_SLOT_COUNT) {
      throw new IllegalArgumentException(
          "BookingRange no puede superar los %d slots (3 horas), pero tiene %d"
              .formatted(MAX_SLOT_COUNT, slots.size()));
    }
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
