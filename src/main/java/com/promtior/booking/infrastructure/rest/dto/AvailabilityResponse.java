package com.promtior.booking.infrastructure.rest.dto;

import com.promtior.booking.domain.Availability;
import com.promtior.booking.domain.Room;
import java.util.List;

public record AvailabilityResponse(
    Room room, List<TimeSlotResponse> freeSlots, List<TimeSlotResponse> occupiedSlots) {

  public static AvailabilityResponse from(Availability availability) {
    return new AvailabilityResponse(
        availability.room(),
        availability.freeSlots().stream().map(TimeSlotResponse::from).toList(),
        availability.occupiedSlots().stream().map(TimeSlotResponse::from).toList());
  }
}
