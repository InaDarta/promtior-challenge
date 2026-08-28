package com.promtior.booking.infrastructure.rest;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promtior.booking.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica el criterio de aceptación de E03.1: login válido devuelve token, API sin token = 401.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest extends AbstractPostgresIntegrationTest {

  private static final String PASSWORD_DEL_ENUNCIADO = "TechnicalChallengePromtior";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void loginConUser1YElPasswordDelEnunciadoDevuelveUnTokenValido() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody("User1", PASSWORD_DEL_ENUNCIADO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value(not("")));
  }

  @Test
  void loginConPasswordIncorrectaDevuelve401() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody("User1", "password-incorrecta")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginConUsuarioInexistenteDevuelve401() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody("NoExiste", PASSWORD_DEL_ENUNCIADO)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void unEndpointDeReservasSinTokenDevuelve401() throws Exception {
    mockMvc.perform(get("/api/bookings")).andExpect(status().isUnauthorized());
  }

  @Test
  void unEndpointDeReservasConTokenValidoNoDevuelve401() throws Exception {
    String token = login("User1", PASSWORD_DEL_ENUNCIADO);

    mockMvc
        .perform(get("/api/bookings").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  private String login(String username, String password) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(username, password)))
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
