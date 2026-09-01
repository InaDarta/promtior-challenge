package com.promtior.booking.infrastructure.llm.dto;

import com.promtior.booking.domain.TimeSlot;
import java.time.LocalDateTime;

/** Slot expresado como {@code start}/{@code end} de calendario, legible por el modelo. */
public record TimeSlotSummary(LocalDateTime start, LocalDateTime end) {

  public static TimeSlotSummary from(TimeSlot slot) {
    return new TimeSlotSummary(slot.start(), slot.end());
  }
}
