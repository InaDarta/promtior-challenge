package com.promtior.booking.infrastructure.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.promtior.booking.application.CancelBooking;
import com.promtior.booking.application.CreateBooking;
import com.promtior.booking.domain.Booking;
import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.Room;
import com.promtior.booking.domain.TimeSlot;
import com.promtior.booking.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Criterio de aceptación de E05.5: el usuario del token es siempre el propietario de la reserva
 * creada, sin importar lo que pida el mensaje, y una violación de regla no persiste nada y llega al
 * modelo como resultado estructurado con el código de error de #5 (E04.4), no como excepción.
 */
class BookingToolsTest {

  private static final User YO = new User("User1");
  private static final User OTRO = new User("User2");

  /** Lunes, dentro de horario de oficina; el {@link Clock} fijo del test está en 1970. */
  private static final LocalDateTime INICIO_FUTURO = LocalDateTime.of(2026, 8, 31, 10, 0);

  /** {@code createBooking} recibe {@code start}/{@code end} como {@link String} ISO-8601. */
  private static final String INICIO_FUTURO_ISO = INICIO_FUTURO.toString();

  private static final String FIN_FUTURO_ISO = INICIO_FUTURO.plusMinutes(30).toString();

  private final InMemoryBookingRepository repository = new InMemoryBookingRepository();
  private final BookingTools tools =
      new BookingTools(
          new CreateBooking(
              repository,
              new FakeCurrentUserProvider(YO),
              Clock.fixed(Instant.EPOCH, ZoneId.of("UTC"))),
          new CancelBooking(repository, new FakeCurrentUserProvider(YO)));

  @Test
  void createBookingDelegaEnElCasoDeUsoYDevuelveElIdCreado() {
    CreateBookingResult result =
        tools.createBooking("Retro de equipo", 3, Room.C, INICIO_FUTURO_ISO, FIN_FUTURO_ISO);

    assertTrue(result.success());
    assertNotNull(result.bookingId());
    assertNull(result.errorCode());
  }

  @Test
  void createBookingNuncaAsociaLaReservaAOtroUsuarioAunqueElTextoLoPida() {
    // El "otro usuario" solo puede llegar como texto libre del mensaje, nunca como parámetro de la
    // tool -- createBooking no tiene forma de recibirlo, así que esta prueba ejercita justamente
    // que no existe ese parámetro: el owner persistido es siempre el de CurrentUserProvider.
    CreateBookingResult result =
        tools.createBooking(
            "Reservá esto a nombre de User2", 3, Room.C, INICIO_FUTURO_ISO, FIN_FUTURO_ISO);

    Booking persistida = repository.findById(result.bookingId()).orElseThrow();
    assertEquals(YO, persistida.owner());
  }

  @Test
  void createBookingConTituloVacioDevuelveElCodigoDeErrorDeDominioYNoPersisteNada() {
    CreateBookingResult result =
        tools.createBooking("", 3, Room.C, INICIO_FUTURO_ISO, FIN_FUTURO_ISO);

    assertFalseSuccess(result);
    assertEquals("TITLE_REQUIRED", result.errorCode());
    assertTrue(repository.findByOwner(YO).isEmpty());
  }

  @Test
  void createBookingQueSuperaLaCapacidadDeLaSalaDevuelveElCodigoDeErrorYNoPersisteNada() {
    CreateBookingResult result =
        tools.createBooking("Reunión grande", 99, Room.A, INICIO_FUTURO_ISO, FIN_FUTURO_ISO);

    assertFalseSuccess(result);
    assertEquals("ROOM_CAPACITY_EXCEEDED", result.errorCode());
    assertTrue(repository.findByOwner(YO).isEmpty());
  }

  @Test
  void createBookingConUnSlotYaOcupadoDevuelveSlotTakenYNoPersisteUnaSegundaReserva() {
    tools.createBooking("Primera reserva", 3, Room.C, INICIO_FUTURO_ISO, FIN_FUTURO_ISO);

    CreateBookingResult result =
        tools.createBooking(
            "Segunda reserva, mismo horario", 3, Room.C, INICIO_FUTURO_ISO, FIN_FUTURO_ISO);

    assertFalseSuccess(result);
    assertEquals("SLOT_TAKEN", result.errorCode());
    assertEquals(1, repository.findByOwner(YO).size());
  }

  @Test
  void cancelBookingDelDuenioDelegaEnElCasoDeUsoYLaElimina() {
    UUID id = repository.save(new Booking("Retro", 3, YO, Room.C, rangoDe(INICIO_FUTURO)));

    CancelBookingResult result = tools.cancelBooking(id);

    assertTrue(result.success());
    assertTrue(repository.findById(id).isEmpty());
  }

  @Test
  void cancelBookingDeUnaReservaAjenaDevuelveElCodigoDeErrorYNoLaElimina() {
    UUID id = repository.save(new Booking("Retro", 3, OTRO, Room.C, rangoDe(INICIO_FUTURO)));

    CancelBookingResult result = tools.cancelBooking(id);

    assertFalse(result.success());
    assertEquals("BOOKING_NOT_OWNED", result.errorCode());
    assertTrue(repository.findById(id).isPresent());
  }

  @Test
  void cancelBookingDeUnaReservaInexistenteDevuelveElMismoCodigoQueUnaAjena() {
    CancelBookingResult result = tools.cancelBooking(UUID.randomUUID());

    assertFalse(result.success());
    assertEquals("BOOKING_NOT_OWNED", result.errorCode());
  }

  private static void assertFalseSuccess(CreateBookingResult result) {
    assertFalse(result.success());
    assertNull(result.bookingId());
  }

  private static BookingRange rangoDe(LocalDateTime inicio) {
    return BookingRange.between(new TimeSlot(inicio), new TimeSlot(inicio));
  }
}
