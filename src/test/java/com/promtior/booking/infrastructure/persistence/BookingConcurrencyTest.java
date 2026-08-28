package com.promtior.booking.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.promtior.booking.AbstractPostgresIntegrationTest;
import com.promtior.booking.application.BookingConflictException;
import com.promtior.booking.application.BookingRepository;
import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.TimeSlot;
import com.promtior.booking.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dos hilos reservan el mismo slot a la vez: el constraint de exclusión de Postgres tiene que
 * garantizar que exactamente uno gane, sin depender de ningún chequeo de disponibilidad en código.
 *
 * <p>{@code NOT_SUPPORTED} desactiva el rollback transaccional que {@code @DataJpaTest} aplica por
 * defecto: cada hilo necesita que su INSERT haga commit de verdad para que el segundo choque contra
 * una fila que el primero ya persistió, así que la limpieza de datos es manual (ver {@link
 * #limpiar()}).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaBookingRepository.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BookingConcurrencyTest extends AbstractPostgresIntegrationTest {

  @Autowired private BookingRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private static final User OWNER_1 = new User("concurrency-user-1");
  private static final User OWNER_2 = new User("concurrency-user-2");
  private static final BookingRange RANGE =
      BookingRange.between(
          new TimeSlot(LocalDateTime.of(2026, 8, 31, 14, 0)),
          new TimeSlot(LocalDateTime.of(2026, 8, 31, 14, 30)));

  @BeforeEach
  void seedUsuarios() {
    jdbcTemplate.update(
        "INSERT INTO app_user (username, password_hash) VALUES (?, 'x'), (?, 'x')",
        OWNER_1.username(),
        OWNER_2.username());
  }

  @AfterEach
  void limpiar() {
    jdbcTemplate.update(
        "DELETE FROM booking WHERE owner_username IN (?, ?)",
        OWNER_1.username(),
        OWNER_2.username());
    jdbcTemplate.update(
        "DELETE FROM app_user WHERE username IN (?, ?)", OWNER_1.username(), OWNER_2.username());
  }

  @Test
  void dosReservasConcurrentesSobreElMismoSlotSoloUnaGana() throws Exception {
    Booking bookingUno = new Booking("Reserva de hilo 1", 2, OWNER_1, Room.A, RANGE);
    Booking bookingDos = new Booking("Reserva de hilo 2", 2, OWNER_2, Room.A, RANGE);

    CountDownLatch listos = new CountDownLatch(2);
    CountDownLatch arranque = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<Optional<BookingConflictException>> resultadoUno =
          executor.submit(intentar(bookingUno, listos, arranque));
      Future<Optional<BookingConflictException>> resultadoDos =
          executor.submit(intentar(bookingDos, listos, arranque));

      listos.await();
      arranque.countDown();

      List<Optional<BookingConflictException>> resultados =
          List.of(resultadoUno.get(10, TimeUnit.SECONDS), resultadoDos.get(10, TimeUnit.SECONDS));

      assertEquals(1, resultados.stream().filter(Optional::isEmpty).count());
      assertEquals(1, resultados.stream().filter(Optional::isPresent).count());

      BookingConflictException error =
          resultados.stream().flatMap(Optional::stream).findFirst().orElseThrow();
      assertEquals(Room.A, error.conflict().room());
      assertTrue(!error.conflict().message().isBlank());

      assertEquals(1, repository.findByRoom(Room.A).size());
    } finally {
      executor.shutdownNow();
    }
  }

  /** La reserva perdedora devuelve su {@link BookingConflictException} en vez de propagarla. */
  private Callable<Optional<BookingConflictException>> intentar(
      Booking booking, CountDownLatch listos, CountDownLatch arranque) {
    return () -> {
      listos.countDown();
      arranque.await();
      try {
        repository.save(booking);
        return Optional.empty();
      } catch (BookingConflictException e) {
        return Optional.of(e);
      }
    };
  }
}
