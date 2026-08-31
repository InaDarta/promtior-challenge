package com.promtior.booking.infrastructure.rest.dto;

import com.promtior.booking.domain.TimeSlot;
import java.time.LocalDateTime;

public record TimeSlotResponse(LocalDateTime start, LocalDateTime end) {

  public static TimeSlotResponse from(TimeSlot slot) {
    return new TimeSlotResponse(slot.start(), slot.end());
  }
}
