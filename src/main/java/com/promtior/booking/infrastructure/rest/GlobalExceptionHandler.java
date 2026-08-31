package com.promtior.booking.infrastructure.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Red de contención para cualquier excepción que ningún otro {@code @RestControllerAdvice} de este
 * paquete supo traducir: Spring resuelve siempre el handler más específico primero, así que este
 * solo corre ante algo realmente no anticipado (un bug, un fallo de infraestructura). A diferencia
 * de esos casos -- esperables y silenciosos o en DEBUG --, acá el log es en ERROR con el stack
 * trace completo: es la señal de que algo rompió y hay que mirarlo.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> onUnexpectedException(Exception e) {
    log.error("Excepción no anticipada", e);
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado.");
    return ResponseEntity.status(problem.getStatus())
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }
}
