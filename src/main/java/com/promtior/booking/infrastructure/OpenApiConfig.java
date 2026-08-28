package com.promtior.booking.infrastructure;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata de la API y el esquema de seguridad que Swagger UI ofrece para autorizar con el JWT
 * devuelto por {@code /api/auth/login} (ADR 0006).
 */
@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "Booking Agent API",
            version = "v1",
            description =
                "Reserva de salas de reunión: login, consulta de agenda, reserva, listado y"
                    + " cancelación."))
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
class OpenApiConfig {}
