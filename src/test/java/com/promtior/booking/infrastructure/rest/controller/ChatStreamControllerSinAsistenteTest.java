package com.promtior.booking.infrastructure.rest.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promtior.booking.AbstractPostgresIntegrationTest;
import com.promtior.booking.infrastructure.rest.dto.ChatRequest;
import com.promtior.booking.infrastructure.rest.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Sin un perfil de proveedor de LLM activo (mismo contexto que {@link
 * ChatControllerSinAsistenteTest}), {@code POST /api/chat/stream} sigue devolviendo {@code 200} --
 * la respuesta ya se comprometió como streaming antes de que el fallo ocurra -- pero con un evento
 * {@code error} in-band, a diferencia del {@code 503} de {@code POST /api/chat}. Ver el Javadoc de
 * {@link ChatController#chatStream}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChatStreamControllerSinAsistenteTest extends AbstractPostgresIntegrationTest {

  private static final String PASSWORD_DEL_ENUNCIADO = "TechnicalChallengePromtior";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void sinAsistenteConfiguradoElStreamDevuelve200ConUnEventoDeError() throws Exception {
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

    assertTrue(body.contains("event:error"));
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
}
