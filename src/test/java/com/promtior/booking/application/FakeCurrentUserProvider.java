package com.promtior.booking.application;

import com.promtior.booking.domain.User;

/** Doble de {@link CurrentUserProvider} que siempre devuelve el usuario con el que se construyó. */
class FakeCurrentUserProvider implements CurrentUserProvider {

  private final User user;

  FakeCurrentUserProvider(User user) {
    this.user = user;
  }

  @Override
  public User currentUser() {
    return user;
  }
}
