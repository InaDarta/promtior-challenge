package com.promtior.booking.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.promtior.booking.AbstractPostgresIntegrationTest;
import com.promtior.booking.application.BookingRepository;
import com.promtior.booking.application.IdentifiedBooking;
import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.TimeSlot;
import com.promtior.booking.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Corre sobre Testcontainers, no sobre H2: {@link AbstractPostgresIntegrationTest}. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaBookingRepository.class)
class BookingRepositoryTest extends AbstractPostgresIntegrationTest {

  @Autowired private BookingRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private static final User OWNER = new User("user1");
  private static final BookingRange RANGE =
      BookingRange.between(
          new TimeSlot(LocalDateTime.of(2026, 8, 31, 10, 0)),
          new TimeSlot(LocalDateTime.of(2026, 8, 31, 10, 30)));

  /**
   * {@code room} ya viene poblada por el seed de E02.2 (Flyway); acá solo hace falta el {@code
   * app_user} que la FK de {@code booking} exige.
   */
  @BeforeEach
  void seedReferenciaDeLaFk() {
    jdbcTemplate.update("INSERT INTO app_user (username, password_hash) VALUES ('user1', 'x')");
    jdbcTemplate.update("INSERT INTO app_user (username, password_hash) VALUES ('user2', 'x')");
  }

  @Test
  void guardaYRecuperaUnaReservaPorSala() {
    Booking booking = new Booking("Retro de equipo", 3, OWNER, Room.C, RANGE);

    repository.save(booking);
    List<Booking> encontradas = repository.findByRoom(Room.C);

    assertEquals(1, encontradas.size());
    assertEquals(booking, encontradas.get(0));
  }

  @Test
  void noDevuelveReservasDeOtraSala() {
    repository.save(new Booking("Retro de equipo", 3, OWNER, Room.C, RANGE));

    assertTrue(repository.findByRoom(Room.D).isEmpty());
  }

  @Test
  void guardaYRecuperaUnaReservaPorPropietario() {
    Booking booking = new Booking("Retro de equipo", 3, OWNER, Room.C, RANGE);

    UUID id = repository.save(booking);
    List<IdentifiedBooking> encontradas = repository.findByOwner(OWNER);

    assertEquals(1, encontradas.size());
    assertEquals(new IdentifiedBooking(id, booking), encontradas.get(0));
  }

  @Test
  void noDevuelveReservasDeOtroPropietario() {
    repository.save(new Booking("Retro de equipo", 3, OWNER, Room.C, RANGE));

    assertTrue(repository.findByOwner(new User("user2")).isEmpty());
  }

  @Test
  void saveDevuelveElIdConElQueLuegoSePuedeEncontrarLaReserva() {
    Booking booking = new Booking("Retro de equipo", 3, OWNER, Room.C, RANGE);

    UUID id = repository.save(booking);

    assertEquals(Optional.of(booking), repository.findById(id));
  }

  @Test
  void findByIdDevuelveVacioSiNoExiste() {
    assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
  }

  @Test
  void deleteByIdEliminaLaReserva() {
    UUID id = repository.save(new Booking("Retro de equipo", 3, OWNER, Room.C, RANGE));

    repository.deleteById(id);

    assertTrue(repository.findById(id).isEmpty());
  }
}
