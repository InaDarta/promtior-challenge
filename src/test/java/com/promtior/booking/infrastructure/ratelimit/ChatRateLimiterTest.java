package com.promtior.booking.infrastructure.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ChatRateLimiterTest {

  @Test
  void permiteHastaElCupoPorUsuarioYLuegoRechaza() {
    ChatRateLimiter limiter =
        new ChatRateLimiter(
            new RateLimitProperties(
                new RateLimitProperties.Limit(100, 100, Duration.ofHours(1)),
                new RateLimitProperties.Limit(2, 2, Duration.ofHours(1))));

    assertTrue(limiter.check("alice").allowed());
    assertTrue(limiter.check("alice").allowed());

    RateLimitDecision tercerIntento = limiter.check("alice");
    assertFalse(tercerIntento.allowed());
    assertEquals("user", tercerIntento.exceededScope());
  }

  @Test
  void elCupoPorUsuarioEsIndependientePorUsuario() {
    ChatRateLimiter limiter =
        new ChatRateLimiter(
            new RateLimitProperties(
                new RateLimitProperties.Limit(100, 100, Duration.ofHours(1)),
                new RateLimitProperties.Limit(1, 1, Duration.ofHours(1))));

    assertTrue(limiter.check("alice").allowed());
    assertFalse(limiter.check("alice").allowed());
    assertTrue(limiter.check("bob").allowed());
  }

  @Test
  void elCupoGlobalRechazaAunConCupoDeUsuarioDisponible() {
    ChatRateLimiter limiter =
        new ChatRateLimiter(
            new RateLimitProperties(
                new RateLimitProperties.Limit(1, 1, Duration.ofHours(1)),
                new RateLimitProperties.Limit(5, 5, Duration.ofHours(1))));

    assertTrue(limiter.check("alice").allowed());

    RateLimitDecision rechazoDeBob = limiter.check("bob");
    assertFalse(rechazoDeBob.allowed());
    assertEquals("global", rechazoDeBob.exceededScope());
  }

  /**
   * Bob tiene capacity 1 en su propio cupo. Si el rechazo por cupo global de su primer intento le
   * hubiera consumido ese cupo igual, este segundo intento fallaría por "user" en vez de volver a
   * fallar por "global".
   */
  @Test
  void unRechazoPorCupoGlobalNoLeConsumeElCupoAlUsuario() {
    ChatRateLimiter limiter =
        new ChatRateLimiter(
            new RateLimitProperties(
                new RateLimitProperties.Limit(1, 1, Duration.ofHours(1)),
                new RateLimitProperties.Limit(1, 1, Duration.ofHours(1))));

    limiter.check("alice");
    limiter.check("bob");
    RateLimitDecision segundoIntentoDeBob = limiter.check("bob");

    assertFalse(segundoIntentoDeBob.allowed());
    assertEquals("global", segundoIntentoDeBob.exceededScope());
  }
}
