package com.promtior.booking.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Corta {@code /api/chat} y {@code /api/chat/stream} con 429 antes de que lleguen al controller
 * cuando {@link ChatRateLimiter} dice que no hay cupo -- así {@code chatStream} nunca llega a abrir
 * el {@code SseEmitter}, que es el único momento en que ya no se puede reescribir el status HTTP.
 * Se registra después de {@code JwtAuthenticationFilter} (necesita el usuario autenticado) pero
 * antes de la cadena de autorización: una request sin token válido todavía no tiene {@link
 * Authentication} en este punto, así que pasa de largo -- la rechaza el 401 de siempre, nunca llega
 * al LLM.
 */
@Component
class ChatRateLimitFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(ChatRateLimitFilter.class);

  private final ChatRateLimiter rateLimiter;
  private final ObjectMapper objectMapper;

  ChatRateLimitFilter(ChatRateLimiter rateLimiter, ObjectMapper objectMapper) {
    this.rateLimiter = rateLimiter;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/chat");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      filterChain.doFilter(request, response);
      return;
    }

    RateLimitDecision decision = rateLimiter.check(authentication.getName());
    if (!decision.allowed()) {
      log.warn(
          "Rate limit ({}) alcanzado para el usuario {}",
          decision.exceededScope(),
          authentication.getName());
      reject(response, decision);
      return;
    }
    filterChain.doFilter(request, response);
  }

  private void reject(HttpServletResponse response, RateLimitDecision decision) throws IOException {
    long retryAfterSeconds = Math.max(1, decision.retryAfter().toSeconds());
    String detail =
        "global".equals(decision.exceededScope())
            ? "El asistente alcanzó su límite de uso por ahora. Esperá unos minutos y volvé a"
                + " intentar."
            : "Estás enviando mensajes muy rápido. Esperá un momento antes de volver a intentar.";

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, detail);
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
    objectMapper.writeValue(response.getWriter(), problem);
  }
}
