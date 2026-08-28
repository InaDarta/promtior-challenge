package com.promtior.booking.domain;

import java.util.Objects;

/**
 * Excepción que lanza el dominio para propagar una violación de {@link BookingError} intacta, en
 * vez de aplanarla a un mensaje de texto: quien la atrapa recupera el {@link BookingError} concreto
 * con todos sus datos de causa.
 */
public final class BookingErrorException extends RuntimeException {

  private final BookingError error;

  public BookingErrorException(BookingError error) {
    super(error.message());
    this.error = Objects.requireNonNull(error, "error");
  }

  public BookingError error() {
    return error;
  }
}
