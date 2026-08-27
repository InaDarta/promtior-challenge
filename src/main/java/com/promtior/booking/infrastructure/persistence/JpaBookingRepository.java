package com.promtior.booking.infrastructure.persistence;

import com.promtior.booking.application.BookingRepository;
import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.Room;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Adaptador de {@link BookingRepository} sobre Spring Data JPA. */
@Repository
class JpaBookingRepository implements BookingRepository {

  private final SpringDataBookingRepository repository;

  JpaBookingRepository(SpringDataBookingRepository repository) {
    this.repository = repository;
  }

  @Override
  public void save(Booking booking) {
    repository.save(BookingJpaEntity.fromDomain(booking));
  }

  @Override
  public List<Booking> findByRoom(Room room) {
    return repository.findByRoomId(room.name()).stream().map(BookingJpaEntity::toDomain).toList();
  }
}
