package com.promtior.booking.application;

import com.promtior.booking.domain.Booking;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancela una reserva propia del usuario autenticado.
 *
 * <p>Cancelar una reserva ajena lanza la misma {@link BookingNotOwnedException} que cancelar una
 * que no existe, para no revelar si existe (ADR 0008).
 */
@Service
public class CancelBooking {

  private final BookingRepository bookingRepository;
  private final CurrentUserProvider currentUserProvider;

  public CancelBooking(
      BookingRepository bookingRepository, CurrentUserProvider currentUserProvider) {
    this.bookingRepository = Objects.requireNonNull(bookingRepository, "bookingRepository");
    this.currentUserProvider = Objects.requireNonNull(currentUserProvider, "currentUserProvider");
  }

  @Transactional
  public void execute(UUID id) {
    Objects.requireNonNull(id, "id");
    Optional<Booking> booking = bookingRepository.findById(id);
    if (booking.isEmpty() || !booking.get().owner().equals(currentUserProvider.currentUser())) {
      throw new BookingNotOwnedException();
    }
    bookingRepository.deleteById(id);
  }
}
