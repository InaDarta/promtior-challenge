package com.promtior.booking.application;

import com.promtior.booking.domain.Booking;
import java.util.Objects;
import java.util.UUID;

/**
 * Una {@link Booking} junto con el id que le asignó el adaptador de persistencia.
 *
 * <p>El dominio no tiene noción de id (ADR 0004): este par vive en {@code application}, para los
 * puntos que sí necesitan referenciar una reserva puntual -- {@link ListMyBookings} para exponerlo
 * y {@link CancelBooking} para recibirlo (ADR 0008).
 */
public record IdentifiedBooking(UUID id, Booking booking) {

  public IdentifiedBooking {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(booking, "booking");
  }
}
