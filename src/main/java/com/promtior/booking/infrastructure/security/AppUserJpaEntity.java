package com.promtior.booking.infrastructure.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fila de la tabla {@code app_user}: credenciales, no identidad de dominio (ver {@code User}). */
@Entity
@Table(name = "app_user")
public class AppUserJpaEntity {

  @Id private String username;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  protected AppUserJpaEntity() {}

  String getUsername() {
    return username;
  }

  String getPasswordHash() {
    return passwordHash;
  }
}
