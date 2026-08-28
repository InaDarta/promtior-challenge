package com.promtior.booking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** El contexto completo levanta contra un Postgres real y Flyway aplica {@code V1__schema.sql}. */
@SpringBootTest
class BookingAgentApplicationTests extends AbstractPostgresIntegrationTest {

  @Test
  void contextLoads() {}
}
