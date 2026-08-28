package com.promtior.booking;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Postgres real vía Testcontainers para tests que necesitan que Flyway aplique la migración. El
 * contenedor es un único campo estático: las subclases lo comparten en vez de levantar uno cada
 * una.
 */
@Testcontainers
public abstract class AbstractPostgresIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
}
