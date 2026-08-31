package com.promtior.booking.infrastructure.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.promtior.booking.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Criterio de aceptación de E08.3: {@code /actuator/health} es alcanzable sin token (Railway y
 * cualquier monitor externo lo necesitan así, ver ADR 0012) y refleja el estado de la base -- el
 * {@code DataSourceHealthIndicator} de Spring Boot ya se auto-configura con {@code
 * spring-boot-starter-data-jpa} en el classpath, esta verificación es sobre la exposición
 * (permitAll + show-details), no sobre el indicador en sí.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthCheckSecurityTest extends AbstractPostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void elHealthCheckEsPublicoYMuestraElEstadoDeLaBase() throws Exception {
    mockMvc
        .perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components.db.status").value("UP"));
  }
}
