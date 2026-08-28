package com.promtior.booking;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Postgres real vía Testcontainers para tests que necesitan que Flyway aplique la migración.
 *
 * <p>El contenedor arranca una única vez por JVM, en el inicializador estático, en vez de con
 * {@code @Testcontainers @Container} (que lo reinicia en cada clase de test que lo hereda). Ese
 * ciclo de vida por clase es incompatible con el cacheo de {@code ApplicationContext} de Spring:
 * dos clases de test con la misma configuración de slice (como {@code BookingRepositoryTest} y
 * {@code BookingConcurrencyTest}, ambas {@code @DataJpaTest}) comparten el mismo contexto cacheado
 * -- y por lo tanto el mismo {@code DataSource} --, pero un contenedor por clase ya detuvo y
 * reemplazó el original para cuando la segunda clase corre, dejando ese {@code DataSource}
 * apuntando a un contenedor muerto. Un único contenedor para toda la JVM (que Ryuk limpia al
 * terminar) elimina esa desincronización.
 */
public abstract class AbstractPostgresIntegrationTest {

  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  static {
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }
}
