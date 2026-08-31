package com.promtior.booking.infrastructure.rest.controller;

import com.promtior.booking.application.CurrentUserProvider;
import com.promtior.booking.infrastructure.llm.BookingAssistant;
import com.promtior.booking.infrastructure.llm.failover.LlmNotConfiguredException;
import com.promtior.booking.infrastructure.rest.dto.ChatRequest;
import com.promtior.booking.infrastructure.rest.dto.ChatResponse;
import com.promtior.booking.infrastructure.rest.error.ChatExceptionHandler;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Un turno de conversación con el asistente de reservas. La sesión queda atada al usuario
 * autenticado: el {@code memoryId} de {@link BookingAssistant} es su username, resuelto vía {@link
 * CurrentUserProvider}, nunca un valor que viaje en el body -- misma garantía que ya vale para el
 * owner de una reserva (ADR 0007). Se resuelve antes de arrancar el streaming (no en un callback
 * async) para no depender de que el {@code SecurityContext} sobreviva el cambio de hilo.
 *
 * <p>Sin un perfil de proveedor de LLM activo, {@link BookingAssistant#chat} lanza {@code
 * LlmNotConfiguredException}; {@link ChatExceptionHandler} la traduce a un 503 en vez de un 500.
 * {@link #chatStream} no puede hacer lo mismo -- ver su Javadoc.
 */
@RestController
@RequestMapping("/api/chat")
@SecurityRequirement(name = "bearerAuth")
class ChatController {

  /**
   * Techo de una conversación completa, no de un solo token: un proveedor colgado no cuelga la
   * conexión para siempre.
   */
  private static final long STREAM_TIMEOUT_MILLIS = Duration.ofMinutes(2).toMillis();

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

  /**
   * Misma conversación que {@link #chat}, en streaming: un evento {@code token} por cada fragmento
   * parcial, un {@code done} final con el texto completo (no solo una marca de fin -- el cliente lo
   * usa para reemplazar lo acumulado por los tokens, autocorrigiéndose ante cualquier corte de
   * chunk raro), o un {@code error} con un mensaje en español si algo falla.
   *
   * <p>El error va como evento in-band y no como un status HTTP distinto (a diferencia de {@link
   * #chat}): para cuando puede fallar -- recién al resolver el proveedor real dentro del callback
   * async, no al invocar este método -- Spring ya comprometió la respuesta como {@code 200
   * text/event-stream}, y no hay forma de reescribir el status en ese punto.
   */
  @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  SseEmitter chatStream(@Valid @RequestBody ChatRequest request) {
    String memoryId = currentUserProvider.currentUser().username();
    SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
    bookingAssistant
        .chatStream(memoryId, request.message())
        .onPartialResponse(token -> send(emitter, "token", token))
        .onCompleteResponse(
            response -> {
              if (send(emitter, "done", response.aiMessage().text())) {
                emitter.complete();
              }
            })
        .onError(error -> sendErrorAndComplete(emitter, error))
        .start();
    return emitter;
  }

  /**
   * @return si el evento se mandó bien -- {@code false} ya deja el emitter completado con error.
   */
  private static boolean send(SseEmitter emitter, String event, String data) {
    try {
      emitter.send(SseEmitter.event().name(event).data(data));
      return true;
    } catch (IOException e) {
      emitter.completeWithError(e);
      return false;
    }
  }

  private static void sendErrorAndComplete(SseEmitter emitter, Throwable error) {
    String message =
        error instanceof LlmNotConfiguredException
            ? "El asistente no está disponible en este momento."
            : "Ocurrió un error generando la respuesta.";
    if (send(emitter, "error", message)) {
      emitter.complete();
    }
  }
}
