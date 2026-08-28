package com.promtior.booking.infrastructure.llm;

import com.promtior.booking.application.CurrentUserProvider;
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
