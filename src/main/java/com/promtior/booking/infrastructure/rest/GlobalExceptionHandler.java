package com.promtior.booking.infrastructure.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Red de contención para cualquier excepción que ningún otro {@code @RestControllerAdvice} de este
 * paquete supo traducir: Spring resuelve siempre el handler más específico primero, así que este
 * solo corre ante algo realmente no anticipado (un bug, un fallo de infraestructura) o ante una
 * excepción propia de Spring MVC (bean validation, JSON malformado, método HTTP no soportado, etc.)
 * que ya implementa {@link ErrorResponse} -- ya trae su propio status y su propio {@code
 * ProblemDetail}, así que se devuelve tal cual en vez de pisarlo con un 500: antes de este handler,
 * esas excepciones nunca llegaban hasta acá (las resolvía Spring más abajo en la cadena), y un
 * {@code Exception.class} sin este caso especial se las robaba.
 *
 * <p>Solo lo genuinamente no anticipado -- ni {@link ErrorResponse} ni ningún otro handler propio
 * lo tradujo -- es en ERROR con el stack trace completo: es la señal de que algo rompió y hay que
 * mirarlo. Un {@link ErrorResponse} es un error esperable del lado del cliente, igual que los de
 * {@link BookingExceptionHandler}, así que queda en DEBUG.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> onException(Exception e) {
    if (e instanceof ErrorResponse errorResponse) {
      log.debug("Excepción de Spring MVC: {}", e.getMessage());
      return ResponseEntity.status(errorResponse.getStatusCode())
          .headers(errorResponse.getHeaders())
          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
          .body(errorResponse.getBody());
    }

    log.error("Excepción no anticipada", e);
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado.");
    return ResponseEntity.status(problem.getStatus())
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }
}
