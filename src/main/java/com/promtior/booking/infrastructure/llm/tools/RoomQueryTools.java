package com.promtior.booking.infrastructure.llm.tools;

import com.promtior.booking.application.GetRoomSchedule;
import com.promtior.booking.application.ListAvailableRooms;
import com.promtior.booking.domain.QueryRange;
import com.promtior.booking.domain.Room;
import com.promtior.booking.infrastructure.llm.BookingAssistant;
import com.promtior.booking.infrastructure.llm.dto.AvailabilitySummary;
import com.promtior.booking.infrastructure.llm.dto.BookingRanges;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Tools de consulta de salas para {@link BookingAssistant} (RT-04, RT-05): adaptadores finos sobre
 * {@link ListAvailableRooms} y {@link GetRoomSchedule}, sin lógica propia. El tipo {@link Room}
 * como parámetro restringe el esquema a A-E; la traducción de {@code start}/{@code end} a {@link
 * com.promtior.booking.domain.QueryRange} vía {@link BookingRanges} es la misma que usa {@code
 * RoomController}.
 */
@Component
public class RoomQueryTools {

  private final ListAvailableRooms listAvailableRooms;
  private final GetRoomSchedule getRoomSchedule;

  public RoomQueryTools(ListAvailableRooms listAvailableRooms, GetRoomSchedule getRoomSchedule) {
    this.listAvailableRooms = Objects.requireNonNull(listAvailableRooms, "listAvailableRooms");
    this.getRoomSchedule = Objects.requireNonNull(getRoomSchedule, "getRoomSchedule");
  }

  @Tool("Lista las salas libres en un rango horario, opcionalmente filtradas por capacidad mínima")
  List<Room> listAvailableRooms(
      @P("Inicio del rango, formato ISO-8601 (ej. 2026-08-31T10:00:00)") String start,
      @P("Fin del rango, formato ISO-8601 (ej. 2026-08-31T11:00:00)") String end,
      @P(value = "Capacidad mínima que debe soportar la sala", required = false)
          Integer minCapacity) {
    return listAvailableRooms.execute(
        BookingRanges.query(LocalDateTime.parse(start), LocalDateTime.parse(end)), minCapacity);
  }

  @Tool("Agenda de una sala en un rango horario: qué slots están libres y cuáles ocupados")
  AvailabilitySummary getRoomSchedule(
      @P("Sala a consultar, una de A, B, C, D, E") Room room,
      @P("Inicio del rango, formato ISO-8601 (ej. 2026-08-31T10:00:00)") String start,
      @P("Fin del rango, formato ISO-8601 (ej. 2026-08-31T11:00:00)") String end) {
    QueryRange range = BookingRanges.query(LocalDateTime.parse(start), LocalDateTime.parse(end));
    return AvailabilitySummary.from(getRoomSchedule.execute(room, range));
  }
}
