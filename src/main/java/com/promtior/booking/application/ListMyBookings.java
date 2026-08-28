package com.promtior.booking.application;

import com.promtior.booking.domain.Booking;
import java.util.List;
import java.util.Objects;

/**
 * Reservas del usuario autenticado.
 *
 * <p>No está pedido en el enunciado: se agrega porque sin él cancelar una reserva es impracticable
 * -- el usuario no tiene forma de saber qué reservas tiene ni con qué identificador referirse a
 * ellas. La identidad se resuelve vía {@link CurrentUserProvider} (ADR 0007), nunca como argumento
 * de este caso de uso.
 */
public class ListMyBookings {

  private final BookingRepository bookingRepository;
  private final CurrentUserProvider currentUserProvider;

  public ListMyBookings(
      BookingRepository bookingRepository, CurrentUserProvider currentUserProvider) {
    this.bookingRepository = Objects.requireNonNull(bookingRepository, "bookingRepository");
    this.currentUserProvider = Objects.requireNonNull(currentUserProvider, "currentUserProvider");
  }

  public List<Booking> execute() {
    return bookingRepository.findByOwner(currentUserProvider.currentUser());
  }
}
