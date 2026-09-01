-- RN-07 garantizada por la base, no solo por la aplicación: un constraint de exclusión rechaza
-- cualquier INSERT que solape sala + rango con una reserva existente, cerrando la ventana de
-- carrera que un chequeo previo en código deja abierta entre el SELECT de disponibilidad y el
-- INSERT. tsrange(range_start, range_end) usa el default '[)' -- inicio inclusive, fin exclusivo --
-- que coincide con BookingRange.overlaps: dos reservas que solo se tocan en el borde no solapan.
-- btree_gist habilita el operador de igualdad sobre room_id (VARCHAR) dentro de un índice GiST,
-- que por sí solo solo sabe comparar rangos.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE booking
    ADD CONSTRAINT booking_no_overlap
    EXCLUDE USING gist (
        room_id WITH =,
        tsrange(range_start, range_end) WITH &&
    );
