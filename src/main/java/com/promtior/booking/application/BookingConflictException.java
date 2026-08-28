package com.promtior.booking.application;

import com.promtior.booking.domain.BookingError;
import java.util.Objects;

/**
 * {@link BookingRepository#save} no pudo persistir la reserva: el constraint de exclusión de
 * Postgres rechazó el INSERT porque la sala ya tenía una reserva que se solapa con el rango pedido.
 * Traduce esa violación a un {@link BookingError} de dominio en vez de dejar escapar la excepción
 * de JDBC.
 */
public final class BookingConflictException extends RuntimeException {

  private final BookingError.SlotOccupied conflict;

  public BookingConflictException(BookingError.SlotOccupied conflict) {
    super(conflict.message());
    this.conflict = Objects.requireNonNull(conflict, "conflict");
  }

  public BookingError.SlotOccupied conflict() {
    return conflict;
  }
}
