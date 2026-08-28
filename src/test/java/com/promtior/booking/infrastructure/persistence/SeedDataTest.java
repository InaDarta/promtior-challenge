package com.promtior.booking.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.promtior.booking.AbstractPostgresIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Verifica el seed de Flyway ({@code V2__seed_salas_usuarios.sql}): E02.2. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SeedDataTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void lasCincoSalasQuedanConSusCapacidades() {
    List<Map<String, Object>> filas =
        jdbcTemplate.queryForList("SELECT id, capacity FROM room ORDER BY id");

    assertEquals(5, filas.size());
    assertEquals(Map.of("id", "A", "capacity", 4), filas.get(0));
    assertEquals(Map.of("id", "B", "capacity", 6), filas.get(1));
    assertEquals(Map.of("id", "C", "capacity", 8), filas.get(2));
    assertEquals(Map.of("id", "D", "capacity", 12), filas.get(3));
    assertEquals(Map.of("id", "E", "capacity", 20), filas.get(4));
  }

  @Test
  void user1YUser2QuedanConElPasswordHasheado() {
    List<Map<String, Object>> filas =
        jdbcTemplate.queryForList("SELECT username, password_hash FROM app_user ORDER BY username");

    assertEquals(2, filas.size());
    assertEquals("User1", filas.get(0).get("username"));
    assertEquals("User2", filas.get(1).get("username"));
    for (Map<String, Object> fila : filas) {
      String hash = (String) fila.get("password_hash");
      assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"), "no es un hash BCrypt");
      assertFalse(hash.contains("TechnicalChallengePromtior"), "el password no debe ir en claro");
    }
  }
}
