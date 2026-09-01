package com.promtior.booking.infrastructure.rest;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promtior.booking.AbstractPostgresIntegrationTest;
import com.promtior.booking.domain.Room;
import com.promtior.booking.infrastructure.rest.controller.BookingControllerTest;
import com.promtior.booking.infrastructure.rest.dto.CreateBookingRequest;
import com.promtior.booking.infrastructure.rest.dto.LoginRequest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Criterio de aceptación de E07.1: el flujo completo login &rarr; reservar &rarr; listar &rarr;
 * cancelar, encadenado como una única transacción de usuario en vez de repartido entre los tests
 * por endpoint de {@link BookingControllerTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookingEndToEndFlowTest extends AbstractPostgresIntegrationTest {

  private static final String PASSWORD_DEL_ENUNCIADO = "TechnicalChallengePromtior";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void loginReservarListarYCancelarFuncionaDeExtremoAExtremo() throws Exception {
    LocalDateTime start = LocalDateTime.of(2026, 9, 3, 15, 0);
    LocalDateTime end = LocalDateTime.of(2026, 9, 3, 15, 30);

    String token = login("User1");

    mockMvc
        .perform(
            get("/api/rooms/available")
                .header("Authorization", "Bearer " + token)
                .param("start", start.toString())
                .param("end", end.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasItem("A")));

    String bookingId = crearReserva(token, "Reunión de punta a punta", 3, Room.A, start, end);

    mockMvc
        .perform(get("/api/bookings").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id=='" + bookingId + "')]").exists())
        .andExpect(jsonPath("$[?(@.id=='" + bookingId + "')].room").value("A"));

    mockMvc
        .perform(
            get("/api/rooms/available")
                .header("Authorization", "Bearer " + token)
                .param("start", start.toString())
                .param("end", end.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", not(hasItem("A"))));

    mockMvc
        .perform(delete("/api/bookings/{id}", bookingId).header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/bookings").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id=='" + bookingId + "')]").doesNotExist());

    mockMvc
        .perform(
            get("/api/rooms/available")
                .header("Authorization", "Bearer " + token)
                .param("start", start.toString())
                .param("end", end.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasItem("A")));
  }

  private String crearReserva(
      String token,
      String title,
      int attendeeCount,
      Room room,
      LocalDateTime start,
      LocalDateTime end)
      throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/bookings")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreateBookingRequest(title, attendeeCount, room, start, end))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(body).get("id").asText();
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
