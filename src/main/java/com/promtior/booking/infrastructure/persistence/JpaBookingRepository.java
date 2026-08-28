package com.promtior.booking.infrastructure.persistence;

import com.promtior.booking.application.BookingConflictException;
import com.promtior.booking.application.BookingRepository;
import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.BookingError;
import com.promtior.booking.domain.Room;
import java.sql.SQLException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/** Adaptador de {@link BookingRepository} sobre Spring Data JPA. */
@Repository
class JpaBookingRepository implements BookingRepository {

  /** SQLSTATE de Postgres para "exclusion_violation" (Apéndice A de su documentación). */
  private static final String EXCLUSION_VIOLATION_SQLSTATE = "23P01";

  private final SpringDataBookingRepository repository;

  JpaBookingRepository(SpringDataBookingRepository repository) {
    this.repository = repository;
  }

  @Override
  public void save(Booking booking) {
    try {
      // flush, no solo save: el INSERT tiene que golpear la base ahora, dentro de este método,
      // para que la violación del constraint de exclusión se traduzca acá y no escape más tarde
      // al hacer commit de la transacción.
      repository.saveAndFlush(BookingJpaEntity.fromDomain(booking));
    } catch (DataIntegrityViolationException e) {
      if (!isExclusionViolation(e)) {
        throw e;
      }
      throw new BookingConflictException(
          new BookingError.SlotOccupied(booking.room(), booking.range().slots()));
    }
  }

  private static boolean isExclusionViolation(DataIntegrityViolationException e) {
    Throwable cause = e.getMostSpecificCause();
    return cause instanceof SQLException sqlException
        && EXCLUSION_VIOLATION_SQLSTATE.equals(sqlException.getSQLState());
  }

  @Override
  public List<Booking> findByRoom(Room room) {
    return repository.findByRoomId(room.name()).stream().map(BookingJpaEntity::toDomain).toList();
  }
}
