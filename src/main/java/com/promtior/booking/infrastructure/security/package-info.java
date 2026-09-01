/**
 * Configuración de Spring Security y el flujo de autenticación JWT.
 *
 * <p>{@link com.promtior.booking.infrastructure.security.SecurityConfig} arma la cadena de filtros;
 * {@link com.promtior.booking.infrastructure.security.JwtService} emite y valida los tokens; {@link
 * com.promtior.booking.infrastructure.security.AppUserDetailsService} resuelve credenciales contra
 * la tabla {@code app_user}; {@link
 * com.promtior.booking.infrastructure.security.SecurityContextCurrentUserProvider} implementa el
 * puerto {@link com.promtior.booking.application.CurrentUserProvider} que expone esa identidad a
 * los casos de uso.
 */
package com.promtior.booking.infrastructure.security;
