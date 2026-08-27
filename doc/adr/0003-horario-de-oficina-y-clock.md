# 0003. Horario de oficina, rechazo de reservas pasadas y `Clock` inyectable

## Estado
Aceptada (2026-08-27)

## Contexto
El enunciado no dice nada sobre horario de oficina ni sobre qué pasa si alguien pide reservar un
horario que ya pasó. Sin una regla de horario, «listar salas disponibles mañana» devolvería 48
slots por sala (24 horas × 2 slots/hora), la mayoría inútiles. Sin una regla de no-pasado, nada
impide construir una reserva con `start` en 2020. Ambas reglas necesitan una noción de «ahora», y
el dominio (`domain`) debe seguir siendo testeable con reloj fijo, sin depender de la hora real de
ejecución — un test que use `LocalDateTime.now()` directamente es no determinístico y falla según
la hora del día en que corra.

## Decisión
- **Horario de oficina**: lunes a viernes, 08:00 a 20:00, zona `America/Montevideo`. Se modela como
  invariante estructural de `BookingRange` — no depende del reloj, solo de los `TimeSlot` del
  rango — y por lo tanto va en el constructor compacto de `BookingRange`, junto a las demás reglas
  de contigüidad y duración máxima (RN-05). La zona horaria se deja documentada como constante
  pública `BookingRange.OFFICE_ZONE`, para que la capa de infraestructura arme el `Clock` de
  producción con ella (`Clock.system(BookingRange.OFFICE_ZONE)`); el dominio en sí opera sobre
  `LocalDateTime` sin conversión de zona, asumiendo que todo horario que recibe ya está expresado
  en esa zona.
- **Rechazo de reservas pasadas**: a diferencia del horario de oficina, esta regla sí depende del
  instante actual, así que no puede vivir en el constructor canónico de `Booking` (un `record` no
  debería llamar a `Clock.systemDefaultZone()` puertas adentro: rompe la pureza del value object y
  lo vuelve no determinístico). Se agrega `Booking.create(..., Clock clock)`, un factory estático
  que recibe el reloj, valida que `range.start()` no sea anterior a `LocalDateTime.now(clock)` y
  delega en el constructor canónico para el resto de las invariantes.
- **`Clock` inyectable**: se usa `java.time.Clock` de la JDK directamente, sin envolverlo en una
  interfaz propia del dominio. Ya es una abstracción diseñada para esto — `Clock.fixed(...)` para
  tests, `Clock.system(zone)` para producción — y agregar un puerto propio solo duplicaría lo que
  el JDK ya resuelve.

## Alternativas descartadas
- **Meter el chequeo de horario de oficina también en un factory con `Clock`** — se descartó
  porque el horario de oficina no necesita el reloj para nada: es una propiedad del rango en sí
  mismo (día de la semana y hora de los `TimeSlot`), no del instante en que se lo construye. Meterlo
  junto al chequeo de no-pasado hubiera obligado a pasar un `Clock` incluso a tests que no evalúan
  nada relacionado con «ahora».
- **`LocalDateTime.now()` sin reloj inyectado, con `@Disabled` o tolerancias en los tests** — el
  criterio de aceptación de esta issue pide explícitamente tests deterministas con reloj fijo; usar
  la hora real haría que un test de «rechaza un inicio a las 07:30» dependiera de qué día corre CI.
- **Interfaz propia `com.promtior.booking.domain.Clock`** — hubiera sido una envoltura sin valor
  agregado sobre `java.time.Clock`, que ya es inyectable, testeable con `Clock.fixed(...)` y no trae
  ninguna dependencia de framework.
- **Restringir el constructor canónico de `Booking` para forzar el paso por `create(...)`** — se
  descartó porque hubiera obligado a todos los tests existentes de invariantes estructurales
  (título, asistentes, sala) a construir y pasar un `Clock` sin necesitarlo. El constructor canónico
  queda público a propósito para esos casos; `create(...)` es el único punto por el que va a pasar
  la capa de aplicación cuando exista el caso de uso de creación de reservas.

## Consecuencias
Una reserva fuera de horario de oficina es irrepresentable: `new BookingRange(...)` la rechaza sin
importar por dónde se construya. Una reserva con inicio pasado solo se rechaza si se crea a través
de `Booking.create(...)` — el constructor canónico de 5 argumentos sigue permitiendo construir una
reserva "pasada" para tests de otras invariantes, que es la única razón por la que puede seguir
siendo público. Cuando exista el caso de uso de creación de reservas (E04), va a llamar siempre a
`Booking.create(...)` con el `Clock` inyectado por Spring, nunca al constructor canónico
directamente.
