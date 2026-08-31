package com.promtior.booking.infrastructure.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code app.rate-limit.*}: capacidad y velocidad de reposición de los cupos {@code global} y
 * {@code per-user} de {@link ChatRateLimiter}. Ver {@code application.yml} y ADR 0012.
 */
@ConfigurationProperties(prefix = "app.rate-limit")
record RateLimitProperties(Limit global, Limit perUser) {

  record Limit(int capacity, int refillTokens, Duration refillPeriod) {}
}
