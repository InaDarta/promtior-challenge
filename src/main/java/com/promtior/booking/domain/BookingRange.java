package com.promtior.booking.domain;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

  private static final DayOfWeek FIRST_OFFICE_DAY = DayOfWeek.MONDAY;
  private static final DayOfWeek LAST_OFFICE_DAY = DayOfWeek.FRIDAY;
  private static final LocalTime OFFICE_OPENING = LocalTime.of(8, 0);
  private static final LocalTime OFFICE_CLOSING = LocalTime.of(20, 0);

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
    LocalDateTime start = slots.get(0).start();
    LocalDateTime end = slots.get(slots.size() - 1).end();
    DayOfWeek day = start.getDayOfWeek();
    if (day.compareTo(FIRST_OFFICE_DAY) < 0 || day.compareTo(LAST_OFFICE_DAY) > 0) {
      throw new IllegalArgumentException(
          "BookingRange debe caer de lunes a viernes, pero %s es %s".formatted(start, day));
    }
    if (start.toLocalTime().isBefore(OFFICE_OPENING)
        || !end.toLocalDate().equals(start.toLocalDate())
        || end.toLocalTime().isAfter(OFFICE_CLOSING)) {
      throw new IllegalArgumentException(
          "BookingRange debe caer dentro del horario de oficina (%s a %s), pero va de %s a %s"
              .formatted(OFFICE_OPENING, OFFICE_CLOSING, start, end));
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
