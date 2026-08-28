package com.promtior.booking.infrastructure.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.promtior.booking.application.GetRoomSchedule;
import com.promtior.booking.application.ListAvailableRooms;
import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.TimeSlot;
import com.promtior.booking.domain.User;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Prueba el criterio de aceptación de E05.4 para las tools de sala: cada una delega en su caso de
 * uso -- sin reimplementar la lógica de disponibilidad -- y el esquema de {@code room} no admite
 * una sala fuera de A-E.
 */
class RoomQueryToolsTest {

  private static final User OWNER = new User("User1");
  private static final LocalDateTime START = LocalDateTime.of(2026, 8, 31, 10, 0);
  private static final LocalDateTime END = LocalDateTime.of(2026, 8, 31, 11, 0);

  /** Las tools reciben {@code start}/{@code end} como {@link String} ISO-8601. */
  private static final String START_ISO = START.toString();

  private static final String END_ISO = END.toString();

  private final InMemoryBookingRepository repository = new InMemoryBookingRepository();
  private final RoomQueryTools tools =
      new RoomQueryTools(new ListAvailableRooms(repository), new GetRoomSchedule(repository));

  @Test
  void listAvailableRoomsDelegaEnElCasoDeUso() {
    repository.save(
        new Booking(
            "Retro de equipo",
            3,
            OWNER,
            Room.C,
            BookingRange.between(new TimeSlot(START), new TimeSlot(START))));

    List<Room> libres = tools.listAvailableRooms(START_ISO, END_ISO, null);

    assertEquals(List.of(Room.A, Room.B, Room.D, Room.E), libres);
  }

  @Test
  void listAvailableRoomsFiltraPorCapacidadMinima() {
    List<Room> libres = tools.listAvailableRooms(START_ISO, END_ISO, 10);

    assertEquals(List.of(Room.D, Room.E), libres);
  }

  @Test
  void getRoomScheduleDelegaEnElCasoDeUso() {
    repository.save(
        new Booking(
            "Retro de equipo",
            3,
            OWNER,
            Room.C,
            BookingRange.between(new TimeSlot(START), new TimeSlot(START))));

    AvailabilitySummary agenda = tools.getRoomSchedule(Room.C, START_ISO, END_ISO);

    assertEquals(Room.C, agenda.room());
    assertEquals(
        List.of(new TimeSlotSummary(START, START.plusMinutes(30))), agenda.occupiedSlots());
    assertEquals(List.of(new TimeSlotSummary(START.plusMinutes(30), END)), agenda.freeSlots());
  }

  @Test
  void elEsquemaDeGetRoomScheduleNoAdmiteUnaSalaFueraDeAaE() {
    ToolSpecification spec =
        ToolSpecifications.toolSpecificationsFrom(tools).stream()
            .filter(s -> s.name().equals("getRoomSchedule"))
            .findFirst()
            .orElseThrow();

    JsonObjectSchema parameters = spec.parameters();
    Object roomSchema = parameters.properties().get("room");

    JsonEnumSchema enumSchema = assertInstanceOf(JsonEnumSchema.class, roomSchema);
    assertEquals(List.of("A", "B", "C", "D", "E"), enumSchema.enumValues());
  }
}
