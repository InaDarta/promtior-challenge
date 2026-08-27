-- Catálogo de salas: id de una letra (A-E) y capacidad máxima de asistentes.
-- VARCHAR y no CHAR: Hibernate mapea java.lang.String a varchar, y ddl-auto=validate
-- compara tipos exactos contra lo que Flyway ya creó.
CREATE TABLE room (
    id VARCHAR(1) PRIMARY KEY,
    capacity INTEGER NOT NULL CHECK (capacity > 0)
);

-- Usuarios de la aplicación. El username es la identidad de dominio (com.promtior.booking.domain.User).
CREATE TABLE app_user (
    username VARCHAR(50) PRIMARY KEY,
    password_hash VARCHAR(100) NOT NULL
);

-- Reservas. room_id y owner_username son columnas planas (no relaciones JPA): ver ADR 0004.
CREATE TABLE booking (
    id UUID PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    attendee_count INTEGER NOT NULL CHECK (attendee_count > 0),
    owner_username VARCHAR(50) NOT NULL REFERENCES app_user (username),
    room_id VARCHAR(1) NOT NULL REFERENCES room (id),
    range_start TIMESTAMP NOT NULL,
    range_end TIMESTAMP NOT NULL,
    CHECK (range_end > range_start)
);

CREATE INDEX idx_booking_room_range ON booking (room_id, range_start, range_end);
