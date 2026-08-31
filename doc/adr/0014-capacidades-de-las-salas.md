# 0014. Capacidades de las salas: escalera fija en un enum cerrado

## Estado
Aceptada (2026-08-27)

## Contexto
El enunciado exige rechazar una reserva que exceda la capacidad de la sala (RN-03), pero no da los
números concretos ni dice cuántas salas hay ni cómo se modelan — un hueco del enunciado, igual que
el horario de oficina y la zona horaria que resuelve [0003](0003-horario-de-oficina-y-clock.md).
Sin una decisión de producto propia, RN-03 no se puede implementar: no hay capacidad contra la
cual comparar `attendeeCount`.

## Decisión
`Room` es un enum cerrado de cinco salas con capacidad fija: A=4, B=6, C=8, D=12, E=20 — una
escalera creciente pensada para cubrir desde una reunión 1:1 hasta una reunión de equipo completo.
Se persiste en Postgres vía el seed de Flyway (`V2__seed_salas_usuarios.sql`), pero el catálogo en
sí —qué salas existen y su capacidad— no es editable en runtime ni tiene UI de administración. El
constructor compacto de `Booking` rechaza `attendeeCount` fuera de `[1, room.capacity()]`,
lanzando `BookingError.CapacityExceeded`.

## Alternativas descartadas
- **Catálogo de salas configurable (alta/baja/edición de salas)** — el enunciado no pide gestión de
  salas, solo reservarlas; construir un CRUD de salas hubiera sumado una feature no evaluada a
  cambio de complejidad, sin ningún requisito trazable (RT) que la pida.
- **Sin chequeo de capacidad** — el enunciado sí exige explícitamente un límite por sala, aunque no
  dé los números; omitirlo dejaría RN-03 sin implementar y sin sentido.
- **Una única capacidad para todas las salas** — no refleja un catálogo de oficina realista (salas
  chicas para 1:1, salas grandes para equipo completo), y no ejercitaría el camino de rechazo de
  RN-03 de forma interesante durante la demo: con una sola capacidad, todas las reservas la
  respetan o la exceden por igual, sin variedad de casos.

## Consecuencias
RN-03 es representable y verificable con datos fijos y deterministas, útil tanto para tests de
dominio como para el guion de la demo (pedir una reserva de 10 personas en la sala A da un rechazo
predecible). El costo es que agregar o modificar una sala requiere una migración de Flyway y un
nuevo valor de enum, no una operación de negocio — aceptable porque el challenge no pide esa
operación.
