package com.promtior.booking.infrastructure.rest;

/**
 * Cuerpo mínimo de error: un mensaje humano, sin código estable por tipo de violación todavía --
 * ese contrato (E04.4) traduce cada subtipo de {@code BookingError} a un código propio.
 */
record ErrorResponse(String message) {}
