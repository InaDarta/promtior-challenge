package com.promtior.booking.infrastructure.rest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promtior.booking.AbstractPostgresIntegrationTest;
import com.promtior.booking.domain.Room;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Verifica el criterio de aceptación de E04.3: consultar agenda desde la API REST. */
@SpringBootTest
@AutoConfigureMockMvc
class RoomControllerTest extends AbstractPostgresIntegrationTest {

  private static final String PASSWORD_DEL_ENUNCIADO = "TechnicalChallengePromtior";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void salasLibresEnUnRangoSinReservasDevuelveLasCincoSalas() throws Exception {
    String token = login("User1");

    mockMvc
        .perform(
            get("/api/rooms/available")
                .header("Authorization", "Bearer " + token)
                .param("start", "2026-09-02T09:00:00")
                .param("end", "2026-09-02T09:30:00"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(5));
  }

  @Test
  void salasLibresFiltradasPorCapacidadMinimaSoloDevuelveLasQueAlcanzan() throws Exception {
    String token = login("User1");

    mockMvc
        .perform(
            get("/api/rooms/available")
                .header("Authorization", "Bearer " + token)
                .param("start", "2026-09-02T10:00:00")
                .param("end", "2026-09-02T10:30:00")
                .param("minCapacity", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", containsInAnyOrder("D", "E")));
  }

  @Test
  void unaSalaConReservaNoApareceEntreLasDisponibles() throws Exception {
    String token = login("User1");
    LocalDateTime start = LocalDateTime.of(2026, 9, 2, 11, 0);
    LocalDateTime end = LocalDateTime.of(2026, 9, 2, 11, 30);

    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CreateBookingRequest("Ocupa la sala A", 2, Room.A, start, end))))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get("/api/rooms/available")
                .header("Authorization", "Bearer " + token)
                .param("start", "2026-09-02T11:00:00")
                .param("end", "2026-09-02T11:30:00"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", not(hasItem("A"))))
        .andExpect(jsonPath("$.length()").value(4));
  }

  @Test
  void laAgendaDeUnaSalaSeparaSlotsLibresDeOcupados() throws Exception {
    String token = login("User1");
    LocalDateTime start = LocalDateTime.of(2026, 9, 2, 13, 0);
    LocalDateTime end = LocalDateTime.of(2026, 9, 2, 13, 30);

    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CreateBookingRequest("Reunión", 2, Room.B, start, end))))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get("/api/rooms/{room}/schedule", "B")
                .header("Authorization", "Bearer " + token)
                .param("start", "2026-09-02T13:00:00")
                .param("end", "2026-09-02T14:00:00"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.room").value("B"))
        .andExpect(jsonPath("$.occupiedSlots.length()").value(1))
        .andExpect(jsonPath("$.freeSlots.length()").value(1))
        .andExpect(jsonPath("$.occupiedSlots[0].start").value("2026-09-02T13:00:00"));
  }

  @Test
  void salasDisponiblesConEndAnteriorAStartDevuelve400() throws Exception {
    String token = login("User1");

    mockMvc
        .perform(
            get("/api/rooms/available")
                .header("Authorization", "Bearer " + token)
                .param("start", "2026-09-02T10:00:00")
                .param("end", "2026-09-02T09:00:00"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void agendaDeUnaSalaConEndAnteriorAStartDevuelve400() throws Exception {
    String token = login("User1");

    mockMvc
        .perform(
            get("/api/rooms/{room}/schedule", "B")
                .header("Authorization", "Bearer " + token)
                .param("start", "2026-09-02T10:00:00")
                .param("end", "2026-09-02T09:00:00"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void salasLibresEnUnRangoDeTodoElDiaNoTieneLimiteDeDuracion() throws Exception {
    String token = login("User1");

    mockMvc
        .perform(
            get("/api/rooms/available")
                .header("Authorization", "Bearer " + token)
                .param("start", "2026-09-03T08:00:00")
                .param("end", "2026-09-03T20:00:00"))
        .andExpect(status().isOk());
  }

  @Test
  void laAgendaDeUnaSalaEnUnRangoDeTodoElDiaNoTieneLimiteDeDuracion() throws Exception {
    String token = login("User1");

    mockMvc
        .perform(
            get("/api/rooms/{room}/schedule", "B")
                .header("Authorization", "Bearer " + token)
                .param("start", "2026-09-03T08:00:00")
                .param("end", "2026-09-03T20:00:00"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.room").value("B"))
        .andExpect(
            result -> {
              String body = result.getResponse().getContentAsString();
              var json = objectMapper.readTree(body);
              int total = json.get("freeSlots").size() + json.get("occupiedSlots").size();
              assertEquals(24, total);
            });
  }

  @Test
  void agendaDeUnaSalaInexistenteDevuelve400() throws Exception {
    String token = login("User1");

    mockMvc
        .perform(
            get("/api/rooms/{room}/schedule", "Z")
                .header("Authorization", "Bearer " + token)
                .param("start", "2026-09-02T09:00:00")
                .param("end", "2026-09-02T09:30:00"))
        .andExpect(status().isBadRequest());
  }

  private String login(String username) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new LoginRequest(username, PASSWORD_DEL_ENUNCIADO))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(body).get("token").asText();
  }
}
