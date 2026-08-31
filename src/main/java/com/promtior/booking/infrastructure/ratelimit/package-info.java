/**
 * Rate limiting de {@code /api/chat}: dos cupos en memoria (<a
 * href="https://github.com/bucket4j/bucket4j">bucket4j</a>), uno global y uno por usuario
 * autenticado, para que unas pocas conversaciones largas durante la evaluación no agoten el cupo
 * diario del tier gratuito de Gemini (ADR 0009). Ver ADR 0012.
 *
 * <p>{@link com.promtior.booking.infrastructure.ratelimit.ChatRateLimitFilter} se registra en la
 * cadena de {@code SecurityConfig} justo después de {@code JwtAuthenticationFilter}: necesita
 * conocer al usuario autenticado para limitar por usuario, pero tiene que rechazar antes de que el
 * controller arranque el streaming de {@code /api/chat/stream} -- ahí ya no se puede reescribir el
 * status HTTP. {@link com.promtior.booking.infrastructure.ratelimit.ChatRateLimiter} lleva la
 * cuenta con dos {@code Bucket} de bucket4j; sobrevive un único proceso, no un restart ni varias
 * instancias.
 */
package com.promtior.booking.infrastructure.ratelimit;
