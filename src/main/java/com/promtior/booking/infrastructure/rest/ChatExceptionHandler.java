package com.promtior.booking.infrastructure.rest;

import com.promtior.booking.infrastructure.llm.LlmNotConfiguredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Sin un {@code ChatModel} configurado, {@code /api/chat} responde 503 en vez de un 500. */
@RestControllerAdvice
class ChatExceptionHandler {

  @ExceptionHandler(LlmNotConfiguredException.class)
  ResponseEntity<Void> onLlmNotConfigured(LlmNotConfiguredException e) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
  }
}
