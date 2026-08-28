package com.promtior.booking.infrastructure.rest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promtior.booking.AbstractPostgresIntegrationTest;
import com.promtior.booking.infrastructure.llm.EchoHistoryChatModel;
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

/**
 * Verifica el criterio de aceptación de E05.3: dos usuarios tienen conversaciones independientes, y
 * el historial de un mismo usuario permite que un segundo turno se refiera al primero.
 *
 * <p>El {@link ChatModel} de este test es un doble determinista que responde con la concatenación
 * de los mensajes de usuario que trae el request -- así el test cruza la memoria de conversación
 * real (por {@code memoryId}) sin depender de un proveedor de LLM real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ChatControllerTest.AsistenteDePruebaConfig.class)
class ChatControllerTest extends AbstractPostgresIntegrationTest {

  private static final String PASSWORD_DEL_ENUNCIADO = "TechnicalChallengePromtior";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void dosUsuariosDistintosTienenConversacionesIndependientes() throws Exception {
    String tokenUser1 = login("User1");
    String tokenUser2 = login("User2");

    chat(tokenUser1, "Reservame la sala A mañana a las 10");
    String replyUser2 = chat(tokenUser2, "cancelala");

    assertFalse(replyUser2.contains("sala A"));
  }

  @Test
  void elHistorialDeUnUsuarioPermiteQueUnSegundoTurnoSeRefieraAlPrimero() throws Exception {
    String token = login("User1");

    chat(token, "Reservame la sala A mañana a las 10");
    String reply = chat(token, "cancelala");

    assertTrue(reply.contains("sala A"));
    assertTrue(reply.contains("cancelala"));
  }

  @Test
  void unMensajeVacioDevuelve400() throws Exception {
    String token = login("User1");

    mockMvc
        .perform(
            post("/api/chat")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChatRequest(""))))
        .andExpect(status().isBadRequest());
  }

  private String chat(String token, String message) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/chat")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new ChatRequest(message))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(body).get("reply").asText();
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
