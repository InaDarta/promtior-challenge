package com.promtior.booking.infrastructure.rest;

import com.promtior.booking.infrastructure.llm.LlmNotConfiguredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Sin un {@code ChatModel} configurado, {@code /api/chat} responde 503 en vez de un 500. */
@RestControllerAdvice
class ChatExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ChatExceptionHandler.class);

  @ExceptionHandler(LlmNotConfiguredException.class)
  ResponseEntity<Void> onLlmNotConfigured(LlmNotConfiguredException e) {
    log.warn("Petición a /api/chat sin un ChatModel configurado");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
  }
}
