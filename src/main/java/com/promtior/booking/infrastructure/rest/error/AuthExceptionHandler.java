package com.promtior.booking.infrastructure.rest.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Credenciales inválidas en {@code /api/auth/login} son un 401, no un 500. */
@RestControllerAdvice
class AuthExceptionHandler {

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<Void> onAuthenticationException(AuthenticationException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }
}
