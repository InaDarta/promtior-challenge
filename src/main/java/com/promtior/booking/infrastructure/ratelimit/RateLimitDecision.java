package com.promtior.booking.infrastructure.ratelimit;

import java.time.Duration;

/**
 * @param exceededScope {@code "global"} o {@code "user"} -- {@code null} si {@code allowed}. Ver
 *     {@link ChatRateLimiter#check}.
 */
record RateLimitDecision(boolean allowed, String exceededScope, Duration retryAfter) {

  static RateLimitDecision permit() {
    return new RateLimitDecision(true, null, Duration.ZERO);
  }

  static RateLimitDecision deny(String exceededScope, Duration retryAfter) {
    return new RateLimitDecision(false, exceededScope, retryAfter);
  }
}
