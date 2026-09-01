package com.promtior.booking.infrastructure.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promtior.booking.AbstractPostgresIntegrationTest;
import com.promtior.booking.infrastructure.llm.EchoHistoryChatModel;
import com.promtior.booking.infrastructure.rest.dto.ChatRequest;
import com.promtior.booking.infrastructure.rest.dto.LoginRequest;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Criterio de aceptación de E08.3: superar el cupo de {@code /api/chat} devuelve 429 con un mensaje
 * entendible, no un 500. El cupo por usuario se achica a 1 vía propiedades de test -- no hace falta
 * mandar decenas de requests reales para agotarlo.
 *
 * <p>Un solo test method para toda la secuencia: {@code User1}/{@code User2} son los únicos
 * usuarios sembrados (V2__seed_salas_usuarios.sql), y el rate limiter vive como bean singleton del
 * contexto de Spring cacheado entre tests de esta clase -- dos methods independientes reusando el
 * mismo username competirían por el mismo cupo, con un resultado que depende del orden en que JUnit
 * los corra.
 */
@SpringBootTest(
    properties = {
      "app.rate-limit.per-user.capacity=1",
      "app.rate-limit.per-user.refill-tokens=1",
      "app.rate-limit.per-user.refill-period=1h",
      "app.rate-limit.global.capacity=100",
      "app.rate-limit.global.refill-tokens=100",
      "app.rate-limit.global.refill-period=1h"
    })
@AutoConfigureMockMvc
@Import(ChatRateLimitIntegrationTest.AsistenteDePruebaConfig.class)
class ChatRateLimitIntegrationTest extends AbstractPostgresIntegrationTest {

  private static final String PASSWORD_DEL_ENUNCIADO = "TechnicalChallengePromtior";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void superarElCupoPorUsuarioDevuelve429ConMensajeEntendibleYNoAfectaAOtroUsuario()
      throws Exception {
    String tokenUser1 = login("User1");
    String tokenUser2 = login("User2");

    mockMvc.perform(chatRequest(tokenUser1, "hola")).andExpect(status().isOk());

    mockMvc
        .perform(chatRequest(tokenUser1, "hola de nuevo"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").exists());

    mockMvc.perform(chatRequest(tokenUser2, "hola")).andExpect(status().isOk());
  }

  private MockHttpServletRequestBuilder chatRequest(String token, String message) throws Exception {
    return post("/api/chat")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new ChatRequest(message)));
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

  @TestConfiguration
  static class AsistenteDePruebaConfig {

    @Bean
    ChatModel chatModel() {
      return new EchoHistoryChatModel();
    }
  }
}
