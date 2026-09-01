package com.promtior.booking.infrastructure.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promtior.booking.AbstractPostgresIntegrationTest;
import com.promtior.booking.infrastructure.rest.controller.BookingControllerTest;
import com.promtior.booking.infrastructure.rest.dto.LoginRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * E03.3: los cuatro escenarios de autorización y suplantación.
 *
 * <p>El cuarto -- cancelar una reserva de otro usuario devuelve 403, sin distinguirlo de "no
 * existe" -- ya quedó cubierto por {@link BookingControllerTest#cancelarUnaReservaAjenaDevuelve403}
 * y {@link BookingControllerTest#cancelarUnaReservaInexistenteDevuelve403SinRevelarQueNoExiste} al
 * construir el endpoint en E04.3; no se duplica acá.
 */
@SpringBootTest(properties = "app.jwt.secret=" + AuthorizationTest.JWT_SECRET_DE_TEST)
@AutoConfigureMockMvc
class AuthorizationTest extends AbstractPostgresIntegrationTest {

  /**
   * Secreto propio de este test (no el default de {@code application.yml}), para poder firmar acá
   * mismo un token vencido sin depender de qué secreto use producción.
   */
  static final String JWT_SECRET_DE_TEST =
      "1+3i4e8C3sz0BOsZi3rLlmWmRiUd2p7zyvQZI072KThUIuI2vNW9c8RY8d8XWOvv";

  private static final String PASSWORD_DEL_ENUNCIADO = "TechnicalChallengePromtior";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void passwordIncorrectaYUsuarioInexistenteDevuelvenLaMismaRespuesta() throws Exception {
    MvcResult passwordIncorrecta =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody("User1", "password-incorrecta")))
            .andExpect(status().isUnauthorized())
            .andReturn();

    MvcResult usuarioInexistente =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody("NoExiste", PASSWORD_DEL_ENUNCIADO)))
            .andExpect(status().isUnauthorized())
            .andReturn();

    assertEquals(
        passwordIncorrecta.getResponse().getStatus(), usuarioInexistente.getResponse().getStatus());
    assertEquals(
        passwordIncorrecta.getResponse().getContentAsString(),
        usuarioInexistente.getResponse().getContentAsString());
  }

  @Test
  void unTokenVencidoDevuelve401() throws Exception {
    mockMvc
        .perform(get("/api/bookings").header("Authorization", "Bearer " + tokenVencido("User1")))
        .andExpect(status().isUnauthorized());
  }

  /**
   * El body pide crear la reserva "a nombre de User2" -- el mismo ataque que describe el criterio
   * de aceptación de E03 ("reservá esto a nombre de User2") pero en forma de JSON en vez de texto
   * de chat. {@code CreateBookingRequest} no tiene ningún campo de owner: Jackson ignora la
   * propiedad desconocida y {@code CreateBooking} solo puede resolver el dueño vía {@link
   * com.promtior.booking.application.CurrentUserProvider}, nunca desde el body. La misma aserción
   * -- la reserva queda a nombre de quien autenticó el token, nunca del usuario que el contenido
   * del mensaje pida suplantar -- sigue aplicando sin cambios el día que quien invoque el endpoint
   * sea una tool del agente (E05) en vez de este POST directo.
   */
  @Test
  void suplantarAOtroUsuarioEnElBodyNoCambiaElPropietarioDeLaReserva() throws Exception {
    String tokenUser1 = login("User1");
    String bodyConSuplantacion =
        """
        {
          "title": "Suplantacion",
          "attendeeCount": 2,
          "room": "A",
          "start": "2026-09-03T09:00:00",
          "end": "2026-09-03T09:30:00",
          "owner": "User2"
        }
        """;

    MvcResult creada =
        mockMvc
            .perform(
                post("/api/bookings")
                    .header("Authorization", "Bearer " + tokenUser1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(bodyConSuplantacion))
            .andExpect(status().isCreated())
            .andReturn();
    String id = objectMapper.readTree(creada.getResponse().getContentAsString()).get("id").asText();

    mockMvc
        .perform(get("/api/bookings").header("Authorization", "Bearer " + tokenUser1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id=='" + id + "')]").exists());

    String tokenUser2 = login("User2");
    mockMvc
        .perform(get("/api/bookings").header("Authorization", "Bearer " + tokenUser2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id=='" + id + "')]").doesNotExist());
  }

  private String tokenVencido(String username) {
    SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(JWT_SECRET_DE_TEST));
    Instant vencimiento = Instant.now().minus(Duration.ofHours(1));
    return Jwts.builder()
        .subject(username)
        .issuedAt(Date.from(vencimiento.minus(Duration.ofMinutes(5))))
        .expiration(Date.from(vencimiento))
        .signWith(key)
        .compact();
  }

  private String login(String username) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(username, PASSWORD_DEL_ENUNCIADO)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(body).get("token").asText();
  }

  private String loginBody(String username, String password) throws Exception {
    return objectMapper.writeValueAsString(new LoginRequest(username, password));
  }
}
