package com.promtior.booking.infrastructure.rest;

import com.promtior.booking.application.GetRoomSchedule;
import com.promtior.booking.application.ListAvailableRooms;
import com.promtior.booking.domain.Room;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta de salas: cuáles están libres en un rango, y la agenda detallada de una en particular.
 */
@RestController
@RequestMapping("/api/rooms")
@SecurityRequirement(name = "bearerAuth")
class RoomController {

  private final ListAvailableRooms listAvailableRooms;
  private final GetRoomSchedule getRoomSchedule;

  RoomController(ListAvailableRooms listAvailableRooms, GetRoomSchedule getRoomSchedule) {
    this.listAvailableRooms = listAvailableRooms;
    this.getRoomSchedule = getRoomSchedule;
  }

  @GetMapping("/available")
  List<Room> available(
      @RequestParam LocalDateTime start,
      @RequestParam LocalDateTime end,
      @RequestParam(required = false) Integer minCapacity) {
    return listAvailableRooms.execute(BookingRanges.query(start, end), minCapacity);
  }

  @GetMapping("/{room}/schedule")
  AvailabilityResponse schedule(
      @PathVariable Room room, @RequestParam LocalDateTime start, @RequestParam LocalDateTime end) {
    return AvailabilityResponse.from(
        getRoomSchedule.execute(room, BookingRanges.query(start, end)));
  }
}
