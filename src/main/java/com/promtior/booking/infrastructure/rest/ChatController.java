package com.promtior.booking.infrastructure.rest;

import com.promtior.booking.application.CurrentUserProvider;
import com.promtior.booking.infrastructure.llm.BookingAssistant;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Un turno de conversación con el asistente de reservas. La sesión queda atada al usuario
 * autenticado: el {@code memoryId} de {@link BookingAssistant} es su username, resuelto vía {@link
 * CurrentUserProvider}, nunca un valor que viaje en el body -- misma garantía que ya vale para el
 * owner de una reserva (ADR 0007).
 *
 * <p>Sin un perfil de proveedor de LLM activo, {@link BookingAssistant#chat} lanza {@code
 * LlmNotConfiguredException}; {@link ChatExceptionHandler} la traduce a un 503 en vez de un 500.
 */
@RestController
@RequestMapping("/api/chat")
@SecurityRequirement(name = "bearerAuth")
class ChatController {

  private final BookingAssistant bookingAssistant;
  private final CurrentUserProvider currentUserProvider;

  ChatController(BookingAssistant bookingAssistant, CurrentUserProvider currentUserProvider) {
    this.bookingAssistant = bookingAssistant;
    this.currentUserProvider = currentUserProvider;
  }

  @PostMapping
  ChatResponse chat(@Valid @RequestBody ChatRequest request) {
    String memoryId = currentUserProvider.currentUser().username();
    return new ChatResponse(bookingAssistant.chat(memoryId, request.message()));
  }
}
