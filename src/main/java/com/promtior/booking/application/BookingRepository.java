package com.promtior.booking.application;

import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia de {@link Booking}, implementado por un adaptador en {@code
 * infrastructure}.
 *
 * <p>{@code Booking} sigue sin id de dominio (ver ADR 0004): el id que este puerto expone es el
 * generado por el adaptador de persistencia, envuelto en {@link IdentifiedBooking} donde hace falta
 * referenciar una reserva puntual, para que el dominio no gane una noción de persistencia.
 */
public interface BookingRepository {

  /**
   * Persiste {@code booking} y devuelve el id generado. Lanza {@link BookingConflictException} si
   * la sala ya tiene una reserva que se solapa con el rango pedido -- lo garantiza el constraint de
   * exclusión de Postgres, no un chequeo previo en esta capa.
   */
  UUID save(Booking booking);

  /** Las reservas existentes en {@code room}, insumo del cálculo de disponibilidad del dominio. */
  List<Booking> findByRoom(Room room);

  /** Las reservas de {@code owner} junto con su id, insumo de {@link ListMyBookings}. */
  List<IdentifiedBooking> findByOwner(User owner);

  /** La reserva persistida bajo {@code id}, si existe. */
  Optional<Booking> findById(UUID id);

  /** Elimina la reserva persistida bajo {@code id}. */
  void deleteById(UUID id);
}
