package com.promtior.booking.infrastructure.rest.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Credenciales inválidas en {@code /api/auth/login} son un 401, no un 500. */
@RestControllerAdvice
class AuthExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(AuthExceptionHandler.class);

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<Void> onAuthenticationException(AuthenticationException e) {
    // Sin el username ni ningún dato de la request: alcanza para ver el volumen de intentos
    // fallidos sin dejar un log utilizable para enumerar cuentas.
    log.warn("Intento de login rechazado: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }
}
