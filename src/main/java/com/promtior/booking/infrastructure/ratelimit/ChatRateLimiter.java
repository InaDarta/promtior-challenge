package com.promtior.booking.infrastructure.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Un cupo global compartido por todos los usuarios y un cupo por usuario, cada uno con su propio
 * {@link Bucket} de bucket4j (ver ADR 0012). Si el cupo del usuario alcanza pero el global ya se
 * agotó, se le devuelve el token consumido -- no fue él quien vació la cuota, no tiene que pagar
 * por eso en su propio cupo.
 */
@Component
class ChatRateLimiter {

  private final Bucket globalBucket;
  private final RateLimitProperties.Limit perUserLimit;
  private final ConcurrentMap<String, Bucket> perUserBuckets = new ConcurrentHashMap<>();

  ChatRateLimiter(RateLimitProperties properties) {
    this.globalBucket = newBucket(properties.global());
    this.perUserLimit = properties.perUser();
  }

  RateLimitDecision check(String username) {
    Bucket userBucket = perUserBuckets.computeIfAbsent(username, u -> newBucket(perUserLimit));

    ConsumptionProbe userProbe = userBucket.tryConsumeAndReturnRemaining(1);
    if (!userProbe.isConsumed()) {
      return RateLimitDecision.deny("user", Duration.ofNanos(userProbe.getNanosToWaitForRefill()));
    }

    ConsumptionProbe globalProbe = globalBucket.tryConsumeAndReturnRemaining(1);
    if (!globalProbe.isConsumed()) {
      userBucket.addTokens(1);
      return RateLimitDecision.deny(
          "global", Duration.ofNanos(globalProbe.getNanosToWaitForRefill()));
    }

    return RateLimitDecision.permit();
  }

  private static Bucket newBucket(RateLimitProperties.Limit limit) {
    return Bucket.builder()
        .addLimit(
            b ->
                b.capacity(limit.capacity())
                    .refillGreedy(limit.refillTokens(), limit.refillPeriod()))
        .build();
  }
}
