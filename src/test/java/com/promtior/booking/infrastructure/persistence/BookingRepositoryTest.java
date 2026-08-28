package com.promtior.booking.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.promtior.booking.AbstractPostgresIntegrationTest;
import com.promtior.booking.application.BookingRepository;
import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.TimeSlot;
import com.promtior.booking.domain.User;
import java.time.LocalDateTime;
import java.util.List;
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
   * El seed de {@code room}/{@code app_user} es de E02.2; acá alcanza con las filas que las FK de
   * {@code booking} exigen para poder insertar.
   */
  @BeforeEach
  void seedReferenciasDeLaFk() {
    jdbcTemplate.update("INSERT INTO room (id, capacity) VALUES ('C', 8), ('D', 12)");
    jdbcTemplate.update("INSERT INTO app_user (username, password_hash) VALUES ('user1', 'x')");
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
}
