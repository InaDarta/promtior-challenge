package com.promtior.booking.infrastructure.rest;

import com.promtior.booking.application.BookingConflictException;
import com.promtior.booking.application.BookingNotOwnedException;
import com.promtior.booking.domain.BookingErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de {@code domain} y {@code application} a {@code
 * application/problem+json} en vez de dejarlas escapar como un 500. El mapeo fino por subtipo de
 * {@code BookingError}, con un código estable por violación y los datos de su causa, vive en {@link
 * BookingProblems} (contrato de E04.4).
 */
@RestControllerAdvice
class BookingExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(BookingExceptionHandler.class);

  /**
   * No existe o es ajena: ambos casos son un 403 idéntico, nunca un 404, para no revelar si la
   * reserva existe (ADR 0008). El detalle sí queda en el log -- a nivel DEBUG, para no perder esa
   * distinción del lado del soporte aunque la respuesta al cliente la esconda.
   */
  @ExceptionHandler(BookingNotOwnedException.class)
  ResponseEntity<Void> onBookingNotOwned(BookingNotOwnedException e) {
    log.debug("Reserva inexistente o ajena", e);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
  }

  @ExceptionHandler(BookingConflictException.class)
  ResponseEntity<ProblemDetail> onBookingConflict(BookingConflictException e) {
    log.debug("Conflicto de reserva: {}", e.getMessage());
    return problemResponse(BookingProblems.from(e.conflict()));
  }

  /**
   * Violación de una regla de reserva con un {@link com.promtior.booking.domain.BookingError}
   * propio.
   */
  @ExceptionHandler(BookingErrorException.class)
  ResponseEntity<ProblemDetail> onBookingError(BookingErrorException e) {
    log.debug("Regla de reserva violada: {}", e.getMessage());
    return problemResponse(BookingProblems.from(e.error()));
  }

  /** Violación de una invariante de dominio que todavía no tiene un {@code BookingError} propio. */
  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ProblemDetail> onIllegalArgument(IllegalArgumentException e) {
    log.debug("Invariante de dominio violada: {}", e.getMessage());
    return problemResponse(
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage()));
  }

  private static ResponseEntity<ProblemDetail> problemResponse(ProblemDetail problem) {
    return ResponseEntity.status(problem.getStatus())
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }
}
