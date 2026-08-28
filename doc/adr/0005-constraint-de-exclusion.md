# 0005. Constraint de exclusión contra la doble reserva y su traducción a `BookingError`

## Estado
Aceptada (2026-08-27)

## Contexto
RN-07 (no solapamiento en la misma sala) hoy solo se valida en código (`Booking.overlapsWith`,
`Availability.conflict`). Ningún caso de uso de creación existe todavía, pero cuando exista, un
chequeo de disponibilidad seguido de un `INSERT` deja una ventana de carrera: dos requests
concurrentes pueden leer "libre" antes de que ninguno de los dos haya escrito, y ambos reservar el
mismo slot. E02.3 pide que la base cierre esa ventana con un constraint de exclusión de Postgres
(`EXCLUDE USING gist`), y que `JpaBookingRepository.save` traduzca su violación a un error de
dominio en vez de dejar escapar la excepción de JDBC.

## Decisión
- **`EXCLUDE USING gist (room_id WITH =, tsrange(range_start, range_end) WITH &&)`**, habilitado
  por `btree_gist` (necesario para el operador `=` sobre `room_id`, un `VARCHAR`, dentro de un
  índice GiST). `tsrange(...)` usa el bound por defecto `[)` -- inicio inclusive, fin exclusivo --
  que coincide exactamente con `BookingRange.overlaps`: dos reservas que solo se tocan en el borde
  no cuentan como solapadas.
- **Detección de la violación por SQLSTATE, no por nombre de constraint**: `JpaBookingRepository`
  llama `saveAndFlush` (no `save`) para que el `INSERT` golpee la base dentro del método, atrapa
  `DataIntegrityViolationException` y compara `getMostSpecificCause().getSQLState()` contra
  `23P01` (`exclusion_violation` en el Apéndice A de la documentación de Postgres). El código de
  clase `23` (integridad referencial) ya distingue `23P01` de `23503` (FK) o `23514` (check), así
  que no hace falta acoplarse al nombre literal `booking_no_overlap`.
- **También se atrapa `40P01` (`deadlock_detected`), no solo `23P01`**: bajo `EXCLUDE USING gist`
  con inserts concurrentes, Postgres no siempre resuelve el conflicto con la violación limpia de
  exclusión -- a veces ambas transacciones quedan esperando el lock de la fila todavía no
  comprometida de la otra mientras chequean el constraint, y Postgres corta ese ciclo abortando una
  de las dos con un deadlock en vez de un `exclusion_violation`. `CannotAcquireLockException` (la
  excepción de Spring que envuelve ese deadlock) no extiende `DataIntegrityViolationException` --
  extiende `ConcurrencyFailureException` --, así que el `catch` necesita cubrir ambos tipos. Mapear
  ese deadlock puntual al mismo `BookingConflictException` es correcto porque solo ocurre cuando
  efectivamente hay un solapamiento real en vuelo: sin overlap, el chequeo del índice GiST no genera
  esa espera cruzada. Encontrado corriendo `BookingConcurrencyTest` repetidas veces contra Postgres
  real (Testcontainers) -- el ~10% de las corridas lo disparaba; ver #73.
- **El conflicto se traduce a `BookingError.SlotOccupied` con el rango completo pedido**, no con los
  slots exactos que efectivamente chocan: el adaptador solo sabe que el `INSERT` fue rechazado, no
  contra qué reserva exacta. Pedir ese detalle exigiría una consulta adicional después del rechazo,
  reintroduciendo la misma ventana de carrera que el constraint existe para cerrar. Se envuelve en
  `BookingConflictException` (nueva, en `application`), que expone el `SlotOccupied` para que quien
  llame a `save` lo use igual que cualquier otro `BookingError`.

## Alternativas descartadas
- **Matchear el nombre del constraint (`booking_no_overlap`) en vez del SQLSTATE** -- funciona, pero
  acopla el código Java al nombre elegido en la migración SQL; renombrar el constraint en una
  migración futura rompería la traducción en silencio (compilaría, pero dejaría de reconocer el
  conflicto). El SQLSTATE es parte del protocolo de Postgres, no un detalle de nomenclatura.
- **`org.postgresql.util.PSQLException` para leer `getServerErrorMessage().getConstraint()`** --
  hubiera dado el nombre exacto del constraint violado, pero el driver de Postgres está en scope
  `runtime` en el `pom.xml` (`infrastructure` no debería depender en tiempo de compilación del
  driver JDBC concreto). `java.sql.SQLException.getSQLState()` es JDK puro y alcanza para distinguir
  exclusion violation de cualquier otra violación de integridad.
- **Consultar las reservas existentes tras el rechazo para armar un `SlotOccupied` con los slots
  exactos** -- descartada por la razón de arriba: agrega una segunda consulta después del `INSERT`
  fallido, con su propia ventana de carrera (la sala pudo liberarse u ocuparse de nuevo entre el
  rechazo y esa consulta). El rango pedido completo es información suficiente para el mensaje de
  error y no depende de una lectura adicional.
- **Test de concurrencia con `@SpringBootTest` de contexto completo** -- se prefirió extender el
  `@DataJpaTest` + `@Import(JpaBookingRepository.class)` que ya usa `BookingRepositoryTest`, sumando
  `@Transactional(propagation = NOT_SUPPORTED)` para desactivar el rollback automático: cada hilo
  necesita que su `INSERT` haga commit de verdad para que el segundo choque contra una fila que el
  primero ya persistió. Un contexto completo no aporta nada que este setup no tenga y es más lento
  de arrancar.

## Efecto secundario: contenedor de Postgres singleton, no por clase
Agregar `BookingConcurrencyTest` expuso un bug latente en `AbstractPostgresIntegrationTest`: con
`@Testcontainers @Container` en un campo estático, el contenedor se reinicia en cada clase de test
que lo hereda, pero el cacheo de `ApplicationContext` de Spring no se entera de ese reinicio. Dos
clases con la misma configuración de slice (`BookingRepositoryTest` y `BookingConcurrencyTest`,
ambas `@DataJpaTest` + `@Import(JpaBookingRepository.class)`) comparten el mismo contexto cacheado
-- y por lo tanto el mismo `DataSource` --, pero para cuando la segunda corre, su contenedor ya
reemplazó al que el `DataSource` cacheado sigue apuntando, que ya fue detenido. En CI (no en local,
donde el orden de ejecución da otro resultado) esto hizo que `BookingConcurrencyTest` fallara el
100% de las veces con `CannotGetJdbcConnectionException`, apuntando a un puerto de un contenedor ya
muerto. Se cambió `AbstractPostgresIntegrationTest` a un contenedor único por JVM (arranca en un
inicializador estático, sin `@Testcontainers`/`@Container`, con `@DynamicPropertySource` en vez de
`@ServiceConnection`), para que el ciclo de vida del contenedor deje de estar atado al de la clase
de test.

## Consecuencias
La doble reserva es irrepresentable en la base incluso si algún caso de uso futuro olvida llamar a
`Availability.conflict()` antes de guardar: el `INSERT` la rechaza igual. El costo es que
`BookingRepository.save` ya no es una operación que solo pueda fallar por bugs de programación --
puede lanzar `BookingConflictException` como parte de su contrato normal, y todo caller (el caso de
uso de creación de reservas, cuando exista en E04) tiene que manejarla explícitamente en vez de
asumir que `save` siempre tiene éxito tras un chequeo de disponibilidad previo.
