package com.promtior.booking.application;

import com.promtior.booking.domain.User;

/**
 * Resuelve la identidad autenticada de la request en curso.
 *
 * <p>Único puerto por el que un caso de uso puede llegar a saber quién es el usuario actual: nunca
 * lo recibe como argumento desde la capa web, para que el texto de un chat no pueda suplantar a
 * otro usuario invocando una tool con un nombre distinto. Ver ADR 0007.
 */
public interface CurrentUserProvider {

  /**
   * El usuario autenticado que hizo la request en curso.
   *
   * @throws IllegalStateException si no hay una identidad autenticada en el contexto -- no debería
   *     ocurrir en ningún endpoint protegido por {@code SecurityConfig}.
   */
  User currentUser();
}
