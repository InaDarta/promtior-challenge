package com.promtior.booking.infrastructure.rest;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promtior.booking.AbstractPostgresIntegrationTest;
import com.promtior.booking.domain.Room;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Verifica el criterio de aceptación de E04.3: reservar, listar y cancelar desde la API REST. */
@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTest extends AbstractPostgresIntegrationTest {

  private static final String PASSWORD_DEL_ENUNCIADO = "TechnicalChallengePromtior";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void crearUnaReservaValidaDevuelve201ConElIdYLosDatosDeLaReserva() throws Exception {
    String token = login("User1");

    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createBookingBody(
                        "Retro de equipo",
                        3,
                        Room.A,
                        LocalDateTime.of(2026, 9, 1, 9, 0),
                        LocalDateTime.of(2026, 9, 1, 9, 30))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(not("")))
        .andExpect(jsonPath("$.title").value("Retro de equipo"))
        .andExpect(jsonPath("$.room").value("A"));
  }

  @Test
  void crearUnaReservaConTituloVacioDevuelve400ConCodigoTitleRequired() throws Exception {
    String token = login("User1");

    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createBookingBody(
                        "",
                        3,
                        Room.A,
                        LocalDateTime.of(2026, 9, 1, 9, 30),
                        LocalDateTime.of(2026, 9, 1, 10, 0))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("TITLE_REQUIRED"));
  }

  @Test
  void crearUnaReservaQueSuperaLaCapacidadDeLaSalaDevuelve400ConCodigoYLosDatosDeLaCausa()
      throws Exception {
    String token = login("User1");

    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createBookingBody(
                        "Reunión gigante",
                        99,
                        Room.A,
                        LocalDateTime.of(2026, 9, 1, 10, 0),
                        LocalDateTime.of(2026, 9, 1, 10, 30))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value(not("")))
        .andExpect(jsonPath("$.code").value("ROOM_CAPACITY_EXCEEDED"))
        .andExpect(jsonPath("$.room").value("A"))
        .andExpect(jsonPath("$.roomCapacity").value(4))
        .andExpect(jsonPath("$.requestedAttendees").value(99));
  }

  @Test
  void crearDosReservasQueSeSolapanEnLaMismaSalaDevuelve409ConCodigoSlotTaken() throws Exception {
    String token = login("User1");
    LocalDateTime start = LocalDateTime.of(2026, 9, 1, 11, 0);
    LocalDateTime end = LocalDateTime.of(2026, 9, 1, 11, 30);

    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBookingBody("Primera", 2, Room.B, start, end)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBookingBody("Segunda", 2, Room.B, start, end)))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("SLOT_TAKEN"))
        .andExpect(jsonPath("$.room").value("B"));
  }

  @Test
  void crearUnaReservaDeMasDeTresHorasDevuelve400ConCodigoMaxDurationExceeded() throws Exception {
    String token = login("User1");

    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createBookingBody(
                        "Reunión larga",
                        3,
                        Room.A,
                        LocalDateTime.of(2026, 9, 1, 15, 0),
                        LocalDateTime.of(2026, 9, 1, 18, 30))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("MAX_DURATION_EXCEEDED"))
        .andExpect(jsonPath("$.requestedSlotCount").value(7))
        .andExpect(jsonPath("$.maxSlotCount").value(6));
  }

  @Test
  void crearUnaReservaUnSabadoDevuelve400ConCodigoOutsideOfficeHours() throws Exception {
    String token = login("User1");

    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createBookingBody(
                        "Reunión de fin de semana",
                        3,
                        Room.A,
                        LocalDateTime.of(2026, 9, 5, 10, 0),
                        LocalDateTime.of(2026, 9, 5, 10, 30))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("OUTSIDE_OFFICE_HOURS"));
  }

  @Test
  void crearUnaReservaEnElPasadoDevuelve400ConCodigoInThePast() throws Exception {
    String token = login("User1");

    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createBookingBody(
                        "Reunión que ya pasó",
                        3,
                        Room.A,
                        LocalDateTime.of(2020, 1, 6, 10, 0),
                        LocalDateTime.of(2020, 1, 6, 10, 30))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("IN_THE_PAST"));
  }

  /**
   * {@code attendeeCount} inválido lo rechaza la validación de bean del DTO antes de llegar al
   * dominio, así que no lleva el {@code code} del contrato de E04.4 -- ese cubre las violaciones de
   * una regla de negocio, no la forma del request.
   */
  @Test
  void crearUnaReservaConCeroAsistentesDevuelve400DeValidacion() throws Exception {
    String token = login("User1");

    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createBookingBody(
                        "Reunión sin asistentes",
                        0,
                        Room.A,
                        LocalDateTime.of(2026, 9, 1, 16, 0),
                        LocalDateTime.of(2026, 9, 1, 16, 30))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void listarMisReservasSoloDevuelveLasPropias() throws Exception {
    String tokenUser1 = login("User1");
    String tokenUser2 = login("User2");
    String id =
        crearReserva(
            tokenUser1,
            "Reserva de User1",
            2,
            Room.C,
            LocalDateTime.of(2026, 9, 1, 12, 0),
            LocalDateTime.of(2026, 9, 1, 12, 30));

    mockMvc
        .perform(get("/api/bookings").header("Authorization", "Bearer " + tokenUser1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id=='" + id + "')]").exists());

    mockMvc
        .perform(get("/api/bookings").header("Authorization", "Bearer " + tokenUser2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id=='" + id + "')]").doesNotExist());
  }

  @Test
  void cancelarUnaReservaPropiaDevuelve204YDejaDeAparecerEnElListado() throws Exception {
    String token = login("User1");
    String id =
        crearReserva(
            token,
            "A cancelar",
            2,
            Room.D,
            LocalDateTime.of(2026, 9, 1, 13, 0),
            LocalDateTime.of(2026, 9, 1, 13, 30));

    mockMvc
        .perform(delete("/api/bookings/{id}", id).header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/bookings").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id=='" + id + "')]").doesNotExist());
  }

  @Test
  void cancelarUnaReservaAjenaDevuelve403() throws Exception {
    String tokenUser1 = login("User1");
    String tokenUser2 = login("User2");
    String id =
        crearReserva(
            tokenUser1,
            "De User1",
            2,
            Room.E,
            LocalDateTime.of(2026, 9, 1, 14, 0),
            LocalDateTime.of(2026, 9, 1, 14, 30));

    mockMvc
        .perform(delete("/api/bookings/{id}", id).header("Authorization", "Bearer " + tokenUser2))
        .andExpect(status().isForbidden());
  }

  @Test
  void cancelarUnaReservaInexistenteDevuelve403SinRevelarQueNoExiste() throws Exception {
    String token = login("User1");

    mockMvc
        .perform(
            delete("/api/bookings/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
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
                    .content(createBookingBody(title, attendeeCount, room, start, end)))
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

  private String createBookingBody(
      String title, int attendeeCount, Room room, LocalDateTime start, LocalDateTime end)
      throws Exception {
    return objectMapper.writeValueAsString(
        new CreateBookingRequest(title, attendeeCount, room, start, end));
  }
}
