# 0008. Cancelación de reservas: id expuesto solo por el puerto, 403 que no revela existencia

## Estado
Aceptada (2026-08-28)

## Contexto
ADR 0004 dejó pendiente esta decisión: `Booking` no tiene id de dominio, y cuando E04.2
(`CancelBooking`) necesitara referenciar una reserva puntual, "probablemente extienda
`BookingRepository` con un método que exponga el id generado por la entidad, sin necesariamente
llevarlo al dominio". Ese momento llegó. Además, el criterio de aceptación de E04.2 pide que
"cancelar una reserva ajena falla sin revelar si existe", el de E03.3 pide que cancelar una reserva
de otro usuario devuelva 403, y el de E04.3 pide poder "listar y cancelar" desde Swagger UI -- lo
que exige que `ListMyBookings` (E04.1) tenga de dónde sacar el id que después se le pasa a
`CancelBooking`.

## Decisión
- `BookingRepository.save` pasa de `void` a devolver `UUID` (el id que genera
  `BookingJpaEntity.fromDomain`), y se agregan `findById(UUID)`/`deleteById(UUID)`. `Booking` sigue
  siendo un record sin campo `id`: el id vive únicamente en el puerto y en la entidad JPA.
- Se agrega el record `IdentifiedBooking(UUID id, Booking booking)` en `application`, y
  `BookingRepository.findByOwner` pasa a devolverlo en vez de `List<Booking>` -- es el único punto
  de lectura que necesita el id, porque es el único insumo de `ListMyBookings`, y de ahí sale el id
  que un cliente (REST en E04.3, tools en E05) le pasa después a `CancelBooking`.
- `CancelBooking` busca la reserva por id y compara su `owner()` contra
  `CurrentUserProvider.currentUser()`. Si la reserva no existe **o** pertenece a otro usuario, lanza
  el mismo `BookingNotOwnedException` en ambos casos -- no hay forma de que quien llama distinga "no
  existe" de "es ajena" a partir de la respuesta. El controlador REST (E04.3) traduce esa excepción
  a 403 en los dos casos por igual.
- Los límites transaccionales de `CreateBooking`/`CancelBooking` se marcan explícitamente con
  `@Transactional` a nivel de caso de uso, no en el controlador ni en el repositorio: es ahí donde
  vive la operación completa (buscar + validar + persistir/eliminar) que tiene que ser atómica.

## Alternativas descartadas
- **404 para "no existe" y 403 para "es ajena"** — es la respuesta REST más "correcta" en abstracto,
  pero le regala a quien prueba ids al azar una forma de enumerar qué reservas existen sin ser
  dueño de ninguna. Colapsar ambos casos en 403 cierra ese canal.
- **Agregar un campo `id` a `Booking`** — seguiría contaminando el dominio con una noción de
  persistencia que ni `Availability` ni las reglas de creación necesitan, y obligaría a tocar el
  constructor canónico que usan todos los tests de E01. Se descartó por la misma razón que ya dio
  ADR 0004.
- **`findByRoom` también devolviendo `IdentifiedBooking`** — `ListAvailableRooms`/`GetRoomSchedule`
  solo necesitan calcular solapamiento sobre `Availability`, nunca referenciar una reserva puntual;
  cambiar ese método también hubiera sido ruido sin ningún llamador que lo necesite.

## Consecuencias
`CancelBooking` y `ListMyBookings` son los únicos puntos del código de `application` que conocen el
id de una reserva; el resto del dominio y de `Availability` sigue operando sobre `Booking` sin id.
Un cliente que intente adivinar ids de reservas ajenas recibe siempre 403, nunca una pista de si
acertó.
