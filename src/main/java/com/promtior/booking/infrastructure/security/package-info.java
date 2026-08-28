/**
 * Configuración de Spring Security y el flujo de autenticación JWT.
 *
 * <p>{@link com.promtior.booking.infrastructure.security.SecurityConfig} arma la cadena de filtros;
 * {@link com.promtior.booking.infrastructure.security.JwtService} emite y valida los tokens; {@link
 * com.promtior.booking.infrastructure.security.AppUserDetailsService} resuelve credenciales contra
 * la tabla {@code app_user}.
 */
package com.promtior.booking.infrastructure.security;
