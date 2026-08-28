package com.promtior.booking.infrastructure.llm;

import com.promtior.booking.domain.Availability;
import com.promtior.booking.domain.Room;
import java.util.List;

/** {@link Availability} con sus slots expresados como {@code start}/{@code end} de calendario. */
record AvailabilitySummary(
    Room room, List<TimeSlotSummary> freeSlots, List<TimeSlotSummary> occupiedSlots) {

  static AvailabilitySummary from(Availability availability) {
    return new AvailabilitySummary(
        availability.room(),
        availability.freeSlots().stream().map(TimeSlotSummary::from).toList(),
        availability.occupiedSlots().stream().map(TimeSlotSummary::from).toList());
  }
}
