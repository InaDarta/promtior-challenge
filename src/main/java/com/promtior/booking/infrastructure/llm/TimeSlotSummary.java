package com.promtior.booking.infrastructure.llm;

import com.promtior.booking.domain.TimeSlot;
import java.time.LocalDateTime;

/** Slot expresado como {@code start}/{@code end} de calendario, legible por el modelo. */
record TimeSlotSummary(LocalDateTime start, LocalDateTime end) {

  static TimeSlotSummary from(TimeSlot slot) {
    return new TimeSlotSummary(slot.start(), slot.end());
  }
}
