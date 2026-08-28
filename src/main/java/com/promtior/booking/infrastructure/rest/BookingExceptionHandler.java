package com.promtior.booking.infrastructure.rest;

import com.promtior.booking.application.BookingConflictException;
import com.promtior.booking.application.BookingNotOwnedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de {@code application} a códigos HTTP en vez de dejarlas escapar como un
 * 500. El mapeo fino por subtipo de {@code BookingError}, con un código estable por violación, es
 * el contrato de E04.4 -- acá solo se evita el stacktrace.
 */
@RestControllerAdvice
class BookingExceptionHandler {

  /**
   * No existe o es ajena: ambos casos son un 403 idéntico, nunca un 404, para no revelar si la
   * reserva existe (ADR 0008).
   */
  @ExceptionHandler(BookingNotOwnedException.class)
  ResponseEntity<Void> onBookingNotOwned(BookingNotOwnedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
  }

  @ExceptionHandler(BookingConflictException.class)
  ResponseEntity<ErrorResponse> onBookingConflict(BookingConflictException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
  }

  /** Violación de una invariante de dominio (título vacío, fuera de horario, capacidad, etc.). */
  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ErrorResponse> onIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
  }
}
