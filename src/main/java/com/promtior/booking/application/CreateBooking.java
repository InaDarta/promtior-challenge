package com.promtior.booking.application;

import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.Room;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea una reserva a nombre del usuario autenticado.
 *
 * <p>El owner nunca es un parámetro de este método -- solo puede venir de {@link
 * CurrentUserProvider}, para que ningún texto ni parámetro de una tool pueda suplantar a otro
 * usuario (ADR 0007). {@link Booking#create} valida contra las reglas de dominio antes de construir
 * la reserva: una que las viola nunca llega a {@link BookingRepository#save}, así que no deja
 * rastro en la base.
 */
@Service
public class CreateBooking {

  private final BookingRepository bookingRepository;
  private final CurrentUserProvider currentUserProvider;
  private final Clock clock;

  public CreateBooking(
      BookingRepository bookingRepository, CurrentUserProvider currentUserProvider, Clock clock) {
    this.bookingRepository = Objects.requireNonNull(bookingRepository, "bookingRepository");
    this.currentUserProvider = Objects.requireNonNull(currentUserProvider, "currentUserProvider");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Transactional
  public UUID execute(String title, int attendeeCount, Room room, BookingRange range) {
    Booking booking =
        Booking.create(title, attendeeCount, currentUserProvider.currentUser(), room, range, clock);
    return bookingRepository.save(booking);
  }
}
