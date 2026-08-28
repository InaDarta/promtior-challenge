package com.promtior.booking.application;

/**
 * {@link CancelBooking} rechazó la cancelación: la reserva no existe o no pertenece al usuario
 * actual. Deliberadamente un único tipo para ambos casos, para que quien llama no pueda distinguir
 * "no existe" de "es ajena" (ADR 0008).
 */
public final class BookingNotOwnedException extends RuntimeException {}
