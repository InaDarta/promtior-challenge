package com.promtior.booking.infrastructure.llm.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.domain.User;
import com.promtior.booking.infrastructure.llm.FakeCurrentUserProvider;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * El contenido de este prompt es lo único que orienta al modelo: sin tests, un cambio de redacción
 * podría borrar sin querer una de las instrucciones que pide el criterio de aceptación de E05.6
 * (pedir datos faltantes, resolver fechas relativas, no inventar, proponer una alternativa real).
 */
class BookingSystemPromptTest {

  private static Clock fixedClockAt(LocalDateTime dateTime) {
    return Clock.fixed(
        dateTime.atZone(BookingRange.OFFICE_ZONE).toInstant(), BookingRange.OFFICE_ZONE);
  }

  @Test
  void incluyeElUsuarioLogueadoLaFechaActualYElCatalogoDeSalasConCapacidades() {
    LocalDateTime ahora = LocalDateTime.of(2026, 8, 27, 15, 32);
    BookingSystemPrompt prompt =
        new BookingSystemPrompt(fixedClockAt(ahora), new FakeCurrentUserProvider(new User("ana")));

    String system = prompt.apply("cualquier-memory-id");

    assertTrue(system.contains("ana"));
    assertTrue(
        system.contains(ahora.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.of("es"))));
    assertTrue(system.contains("2026"));
    assertTrue(system.contains("15:32"));
    assertTrue(system.contains("America/Montevideo"));
    assertTrue(system.contains("Sala A: 4 personas"));
    assertTrue(system.contains("Sala B: 6 personas"));
    assertTrue(system.contains("Sala C: 8 personas"));
    assertTrue(system.contains("Sala D: 12 personas"));
    assertTrue(system.contains("Sala E: 20 personas"));
  }

  @Test
  void resuelveLaFechaActualPorCadaLlamadaSegunElUsuarioAlMomento() {
    LocalDateTime ahora = LocalDateTime.of(2026, 1, 5, 9, 0);
    BookingSystemPrompt prompt =
        new BookingSystemPrompt(fixedClockAt(ahora), new FakeCurrentUserProvider(new User("beto")));

    String primeraLlamada = prompt.apply("memoria-1");
    String segundaLlamada = prompt.apply("memoria-2");

    assertTrue(primeraLlamada.contains("beto"));
    assertTrue(segundaLlamada.contains("beto"));
    assertTrue(primeraLlamada.contains("2026"));
    assertTrue(segundaLlamada.contains("2026"));
  }

  @Test
  void instruyeReglasDeReservaEnLenguajeLlano() {
    String system = promptDeMuestra();

    assertTrue(system.contains("lunes a viernes"));
    assertTrue(system.contains("8:00 a 20:00"));
    assertTrue(system.contains("30 minutos"));
    assertTrue(system.contains("3 horas"));
  }

  @Test
  void instruyePedirLosDatosFaltantesEnVezDeInventarlos() {
    String system = promptDeMuestra();

    assertTrue(system.contains("preguntalos antes de reservar"));
    assertTrue(system.contains("Nunca inventes un dato"));
  }

  @Test
  void instruyeQueElModeloNoValidaYQueDebeContarLaVerdad() {
    String system = promptDeMuestra();

    assertTrue(system.contains("Vos no aplicás estas reglas"));
  }

  @Test
  void instruyeProponerUnaAlternativaRealAntePedidosImposibles() {
    String system = promptDeMuestra();

    assertTrue(system.contains("consultá con las tools de disponibilidad"));
  }

  private static String promptDeMuestra() {
    BookingSystemPrompt prompt =
        new BookingSystemPrompt(
            Clock.systemDefaultZone(), new FakeCurrentUserProvider(new User("nadie")));
    return prompt.apply("memoria-de-prueba");
  }
}
