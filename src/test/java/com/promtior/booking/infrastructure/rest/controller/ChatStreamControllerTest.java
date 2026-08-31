package com.promtior.booking.infrastructure.rest.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promtior.booking.AbstractPostgresIntegrationTest;
import com.promtior.booking.infrastructure.llm.EchoHistoryStreamingChatModel;
import com.promtior.booking.infrastructure.rest.dto.ChatRequest;
import com.promtior.booking.infrastructure.rest.dto.LoginRequest;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Verifica el vocabulario de eventos SSE de {@code POST /api/chat/stream} (E06.3): un evento {@code
 * token} por cada fragmento parcial, y un {@code done} final con el texto completo. El {@link
 * StreamingChatModel} de este test es el mismo doble determinista que {@link ChatControllerTest}
 * usa para el camino síncrono, adaptado a streaming.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ChatStreamControllerTest.AsistenteDeStreamingDePruebaConfig.class)
class ChatStreamControllerTest extends AbstractPostgresIntegrationTest {

  private static final String PASSWORD_DEL_ENUNCIADO = "TechnicalChallengePromtior";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void elStreamContieneEventosDeTokenYDeFinConElTextoCompleto() throws Exception {
    String token = login("User1");

    MvcResult mvcResult =
        mockMvc
            .perform(
                post("/api/chat/stream")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new ChatRequest("hola"))))
            .andExpect(request().asyncStarted())
            .andReturn();

    String body =
        mockMvc
            .perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertTrue(body.contains("event:token"));
    assertTrue(body.contains("event:done"));
    assertTrue(body.contains("hola"));
  }

  private String login(String username) throws Exception {
    String responseBody =
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
    return objectMapper.readTree(responseBody).get("token").asText();
  }

  @TestConfiguration
  static class AsistenteDeStreamingDePruebaConfig {

    @Bean
    StreamingChatModel streamingChatModel() {
      return new EchoHistoryStreamingChatModel();
    }
  }
}
