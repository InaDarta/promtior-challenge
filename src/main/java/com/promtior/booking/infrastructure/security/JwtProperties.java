package com.promtior.booking.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code app.jwt.*}: clave de firma (HS256, base64) y vigencia del token. Ver {@code
 * application.yml} y ADR 0006.
 */
@ConfigurationProperties(prefix = "app.jwt")
record JwtProperties(String secret, long expirationMinutes) {}
