package com.promtior.booking.infrastructure.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/** Emite y valida los JWT que representan una sesión autenticada. */
@Component
public class JwtService {

  private final SecretKey key;
  private final Duration expiration;

  JwtService(JwtProperties properties) {
    this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(properties.secret()));
    this.expiration = Duration.ofMinutes(properties.expirationMinutes());
  }

  public String generateToken(String username) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(username)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(expiration)))
        .signWith(key)
        .compact();
  }

  /** {@code null} si el token está vencido, mal firmado o mal formado. */
  String extractUsername(String token) {
    try {
      return Jwts.parser()
          .verifyWith(key)
          .build()
          .parseSignedClaims(token)
          .getPayload()
          .getSubject();
    } catch (JwtException | IllegalArgumentException e) {
      return null;
    }
  }
}
