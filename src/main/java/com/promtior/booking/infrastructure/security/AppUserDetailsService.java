package com.promtior.booking.infrastructure.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Resuelve {@link UserDetails} desde {@code app_user}. No hay columna de rol todavía: toda cuenta
 * autenticada recibe {@code ROLE_USER}.
 */
@Service
class AppUserDetailsService implements UserDetailsService {

  private final SpringDataAppUserRepository repository;

  AppUserDetailsService(SpringDataAppUserRepository repository) {
    this.repository = repository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) {
    AppUserJpaEntity entity =
        repository.findById(username).orElseThrow(() -> new UsernameNotFoundException(username));
    return org.springframework.security.core.userdetails.User.withUsername(entity.getUsername())
        .password(entity.getPasswordHash())
        .authorities("ROLE_USER")
        .build();
  }
}
