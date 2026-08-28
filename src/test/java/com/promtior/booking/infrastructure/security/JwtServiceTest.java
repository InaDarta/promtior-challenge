package com.promtior.booking.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET =
      "1+3i4e8C3sz0BOsZi3rLlmWmRiUd2p7zyvQZI072KThUIuI2vNW9c8RY8d8XWOvv";

  private final JwtService jwtService = new JwtService(new JwtProperties(SECRET, 60));

  @Test
  void unTokenRecienEmitidoResuelveAlUsernameQueLoGenero() {
    String token = jwtService.generateToken("User1");

    assertEquals("User1", jwtService.extractUsername(token));
  }

  @Test
  void unTokenVencidoNoResuelveUsername() {
    JwtService serviceConVigenciaNegativa = new JwtService(new JwtProperties(SECRET, -1));

    String token = serviceConVigenciaNegativa.generateToken("User1");

    assertNull(jwtService.extractUsername(token));
  }

  @Test
  void unTokenFirmadoConOtraClaveNoResuelveUsername() {
    JwtService otroService =
        new JwtService(
            new JwtProperties(
                "EOxc7cH0Lu/55QDjJh/ciHPKgQ4lO7WTmKKg28nJbow1nmSKLa5R/exzaA3gDDWd", 60));
    String token = otroService.generateToken("User1");

    assertNull(jwtService.extractUsername(token));
  }

  @Test
  void unTokenMalFormadoNoResuelveUsername() {
    assertNull(jwtService.extractUsername("no-soy-un-jwt"));
  }
}
