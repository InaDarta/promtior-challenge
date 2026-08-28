package com.promtior.booking.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.promtior.booking.domain.User;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextCurrentUserProviderTest {

  private final SecurityContextCurrentUserProvider provider =
      new SecurityContextCurrentUserProvider();

  @AfterEach
  void limpiarContextoDeSeguridad() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void resuelveElUsernameDeLaAutenticacionEnElContexto() {
    autenticarComo("User1");

    assertEquals(new User("User1"), provider.currentUser());
  }

  @Test
  void sinAutenticacionEnElContextoLanzaIllegalStateException() {
    SecurityContextHolder.clearContext();

    assertThrows(IllegalStateException.class, provider::currentUser);
  }

  @Test
  void conUnaAutenticacionNoAutenticadaLanzaIllegalStateException() {
    TestingAuthenticationToken authentication =
        new TestingAuthenticationToken("User1", null, List.of());
    authentication.setAuthenticated(false);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    assertThrows(IllegalStateException.class, provider::currentUser);
  }

  private void autenticarComo(String username) {
    var authentication = new TestingAuthenticationToken(username, null, List.of());
    authentication.setAuthenticated(true);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
