package com.promtior.booking.domain;

import java.util.Objects;

/** Identidad de un usuario del dominio: quien es propietario de una reserva. */
public record User(String username) {

  public User {
    Objects.requireNonNull(username, "username");
    if (username.isBlank()) {
      throw new IllegalArgumentException("username no puede estar vacío ni contener solo espacios");
    }
  }
}
