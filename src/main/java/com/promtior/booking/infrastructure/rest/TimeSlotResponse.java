package com.promtior.booking.infrastructure.rest;

import com.promtior.booking.domain.TimeSlot;
import java.time.LocalDateTime;

record TimeSlotResponse(LocalDateTime start, LocalDateTime end) {

  static TimeSlotResponse from(TimeSlot slot) {
    return new TimeSlotResponse(slot.start(), slot.end());
  }
}
