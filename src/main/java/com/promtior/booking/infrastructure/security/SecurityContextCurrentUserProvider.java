package com.promtior.booking.infrastructure.security;

import com.promtior.booking.application.CurrentUserProvider;
import com.promtior.booking.domain.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Lee la identidad que {@link JwtAuthenticationFilter} dejó en el {@link SecurityContextHolder} y
 * la traduce al {@link User} de dominio. Único punto de resolución de identidad: nada fuera de este
 * adaptador toca el {@code SecurityContext} para averiguar quién es el usuario actual.
 */
@Component
class SecurityContextCurrentUserProvider implements CurrentUserProvider {

  @Override
  public User currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new IllegalStateException("No hay usuario autenticado en el contexto de seguridad");
    }
    return new User(authentication.getName());
  }
}
