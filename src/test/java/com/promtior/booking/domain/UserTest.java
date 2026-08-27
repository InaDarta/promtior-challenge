package com.promtior.booking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void aceptaUnUsernameValido() {
    User user = new User("user1");
    assertEquals("user1", user.username());
  }

  @Test
  void rechazaUsernameNulo() {
    assertThrows(NullPointerException.class, () -> new User(null));
  }

  @Test
  void rechazaUsernameVacio() {
    assertThrows(IllegalArgumentException.class, () -> new User(""));
  }

  @Test
  void rechazaUsernameSoloEspacios() {
    assertThrows(IllegalArgumentException.class, () -> new User("   "));
  }
}
