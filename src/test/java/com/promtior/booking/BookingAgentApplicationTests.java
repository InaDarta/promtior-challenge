package com.promtior.booking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** El contexto completo levanta contra un Postgres real y Flyway aplica todas las migraciones. */
@SpringBootTest
class BookingAgentApplicationTests extends AbstractPostgresIntegrationTest {

  @Test
  void contextLoads() {}
}
