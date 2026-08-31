package com.promtior.booking.infrastructure.persistence;

import com.promtior.booking.application.BookingConflictException;
import com.promtior.booking.application.BookingRepository;
import com.promtior.booking.application.IdentifiedBooking;
import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.BookingError;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.User;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/** Adaptador de {@link BookingRepository} sobre Spring Data JPA. */
@Repository
class JpaBookingRepository implements BookingRepository {

  /**
   * SQLSTATE de Postgres que significan "esta reserva chocó contra otra que se solapa": la
   * violación limpia del constraint de exclusión ({@code exclusion_violation}) y el deadlock que
   * Postgres puede reportar en su lugar cuando dos INSERT concurrentes se solapan y cada uno queda
   * esperando el lock de la fila todavía no comprometida del otro mientras chequea el constraint
   * ({@code deadlock_detected}, Apéndice A de la documentación de Postgres). Ese deadlock puntual
   * solo ocurre cuando efectivamente hay un solapamiento real en vuelo, así que es equivalente al
   * conflicto limpio para quien llama a {@code save}.
   */
  private static final Set<String> CONFLICT_SQLSTATES = Set.of("23P01", "40P01");

  private final SpringDataBookingRepository repository;

  JpaBookingRepository(SpringDataBookingRepository repository) {
    this.repository = repository;
  }

  @Override
  public UUID save(Booking booking) {
    try {
      return repository.saveAndFlush(BookingJpaEntity.fromDomain(booking)).getId();
    } catch (DataIntegrityViolationException | ConcurrencyFailureException e) {
      if (!isConflict(e)) {
        throw e;
      }
      throw new BookingConflictException(
          new BookingError.SlotOccupied(booking.room(), booking.range().slots()));
    }
  }

  private static boolean isConflict(DataAccessException e) {
    Throwable cause = e.getMostSpecificCause();
    return cause instanceof SQLException sqlException
        && CONFLICT_SQLSTATES.contains(sqlException.getSQLState());
  }

  @Override
  public List<Booking> findByRoom(Room room) {
    return repository.findByRoomId(room.name()).stream().map(BookingJpaEntity::toDomain).toList();
  }

  @Override
  public List<IdentifiedBooking> findByOwner(User owner) {
    return repository.findByOwnerUsername(owner.username()).stream()
        .map(entity -> new IdentifiedBooking(entity.getId(), entity.toDomain()))
        .toList();
  }

  @Override
  public Optional<Booking> findById(UUID id) {
    return repository.findById(id).map(BookingJpaEntity::toDomain);
  }

  @Override
  public void deleteById(UUID id) {
    repository.deleteById(id);
  }
}
