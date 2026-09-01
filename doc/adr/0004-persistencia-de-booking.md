# 0004. Persistencia de `Booking`: sin id de dominio, columnas planas para las FK

## Estado
Aceptada (2026-08-27)

## Contexto
E02.1 pide la migración Flyway (`room`, `app_user`, `booking`), las entidades JPA y el mapeo hacia
y desde el dominio, y los adaptadores de los puertos de repositorio. El dominio (`Booking`,
`Room`, `User`) es un `record` sin identidad de persistencia: `Room` es un enum cerrado y `User`
se identifica por `username`. Ninguna épica hasta ahora (E01, este issue) necesita referenciar una
reserva por id — `CancelBooking` y `ListMyBookings` (E04) sí lo van a necesitar, pero todavía no
existen.

## Decisión
- `Booking` no gana un campo `id`: seguiría contaminando el dominio con una noción de persistencia
  que hoy ningún caso de uso necesita. El id vive solo en `BookingJpaEntity`, generado con
  `UUID.randomUUID()` al mapear desde el dominio; `BookingRepository.save` es `void`.
- `BookingJpaEntity.roomId` y `.ownerUsername` son columnas `String` planas, no relaciones JPA
  (`@ManyToOne`) hacia entidades `RoomJpaEntity`/`AppUserJpaEntity`. La integridad referencial la
  garantiza la FK de Postgres (`REFERENCES room(id)`, `REFERENCES app_user(username)`), no
  Hibernate. Como consecuencia, esas dos tablas no tienen entidad JPA propia todavía: nada las
  consulta desde código; `room` y `app_user` existen en el schema para la FK y para que E02.2
  siembre sus filas.
- `BookingRange` se aplana a dos columnas (`range_start`, `range_end`): el inicio del primer slot y
  el fin del último. `BookingRange.between(...)` reconstruye la lista de slots contiguos al leer, sin
  necesidad de guardar cada slot individualmente.
- El único puerto que se define en `application` es `BookingRepository` (`save`, `findByRoom`):
  es el único que un adaptador de esta issue necesita implementar y probar contra Testcontainers.

## Alternativas descartadas
- **Agregar `BookingId`/`UUID id` al record `Booking`** — se descartó porque ningún caso de uso
  actual lo necesita y hubiera obligado a tocar el constructor canónico que ya usan todos los tests
  de E01. Cuando E04.2 (`CancelBooking`) necesite referenciar una reserva puntual, se decide ahí
  cómo exponer el id sin romper la pureza del dominio.
- **`@ManyToOne` de `BookingJpaEntity` hacia `RoomJpaEntity`/`AppUserJpaEntity`** — hubiera exigido
  crear y probar dos entidades y repositorios Spring Data que ningún código usa todavía, solo para
  navegar una relación que el mapeo hacia el dominio no necesita (el dominio solo quiere el `Room`
  enum y el `username`, no el grafo de objetos JPA).
- **`UserRepository`/`RoomRepository` en `application` ya en esta issue** — se descartó por lo
  mismo: nada en el código actual necesita buscar un `User` o `Room` desde persistencia. `E02.2`
  (seed) probablemente inserte esas filas con SQL de migración o un repositorio Spring Data interno
  de infraestructura, sin puerto de dominio; `E03` (auth) resuelve el usuario autenticado con su
  propia consulta (necesita el `password_hash`, que no es un concepto de dominio).

## Consecuencias
Guardar una reserva dos veces crea dos filas distintas (no hay upsert ni concepto de "la misma
reserva" a nivel de persistencia todavía) — aceptable porque el único caso de uso hoy es crear, no
actualizar ni cancelar. Cuando E04.2 necesite cancelar una reserva propia, va a tener que decidir
cómo id-entificar una reserva sin romper esta decisión; probablemente extienda
`BookingRepository` con un método que exponga el id generado por la entidad, sin necesariamente
llevarlo al dominio.
