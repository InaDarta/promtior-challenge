package com.promtior.booking.infrastructure.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * Sin un perfil de proveedor de LLM activo (el caso de este contexto, igual que en {@link
 * BookingControllerTest} y el resto de los tests que no activan {@code gemini}/{@code groq}/{@code
 * ollama}) no hay {@code ChatModel} ni, por lo tanto, {@code BookingAssistant} -- el endpoint debe
 * responder 503 en vez de que falle el arranque de toda la aplicación.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerSinAsistenteTest extends AbstractPostgresIntegrationTest {

  private static final String PASSWORD_DEL_ENUNCIADO = "TechnicalChallengePromtior";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void sinAsistenteConfiguradoElChatDevuelve503() throws Exception {
    String token = login("User1");

    mockMvc
        .perform(
            post("/api/chat")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChatRequest("hola"))))
        .andExpect(status().isServiceUnavailable());
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
