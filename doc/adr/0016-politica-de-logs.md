# 0016. Política de logs

## Estado
Aceptada (2026-08-31)

## Contexto
Una revisión del código encontró que, fuera de `infrastructure.llm` (failover de proveedor y
tracing de Langfuse, ADR 0011), ninguna otra clase logueaba nada: los tres
`@RestControllerAdvice` traducían excepciones a HTTP sin dejar rastro, `ChatRateLimitFilter` no
avisaba al rechazar por 429, y `JwtAuthenticationFilter` tenía un `catch` vacío para un token
válido de un usuario que ya no existe. Sin una decisión explícita de qué nivel usar en cada caso,
la tendencia natural es no loguear nada (como pasó acá) o loguear todo al mismo nivel, lo que en
producción termina siendo tan inútil como no loguear.

## Decisión
`logging.level.com.promtior.booking` en `INFO` por default (`LOG_LEVEL`, subible sin redeploy), y
cuatro niveles con un criterio fijo según qué tan esperable es el caso y a quién le sirve verlo:

- **ERROR** -- algo no anticipado. Único caso: el `@ExceptionHandler(Exception.class)` de
  `GlobalExceptionHandler`, red de contención para lo que ningún otro advice supo traducir. Con
  stack trace completo: es la señal de que hay que mirar algo.
- **WARN** -- una condición operacional o de seguridad, esperable pero que conviene poder ver sin
  tener que prender tracing: failover a un proveedor de respaldo (ya existía, ADR 0011), rate
  limit alcanzado (`ChatRateLimitFilter`), LLM no configurado (`ChatExceptionHandler`, 503) y
  login rechazado (`AuthExceptionHandler`, 401 -- sin el username ni datos de la request, alcanza
  con ver el volumen de intentos fallidos sin dejar un log utilizable para enumerar cuentas).
- **DEBUG** -- resultado de negocio esperado por el uso normal de la app: reserva ajena o
  inexistente, conflicto de horario, regla de reserva violada, invariante de dominio violada
  (los cuatro casos de `BookingExceptionHandler`), y el token de un usuario borrado
  (`JwtAuthenticationFilter`). No son errores del sistema -- pasan todos los días con un cliente
  normal -- así que no van a INFO/WARN en producción, pero quedan disponibles con solo subir
  `LOG_LEVEL` cuando hace falta reconstruir qué le pasó a un usuario puntual.
- **INFO** -- reservado para el arranque de Spring Boot y lo que agreguen features futuras que
  necesiten confirmar "esto pasó" sin ser ni un error ni ruido de cada request.

Sin `MDC` propio: `micrometer-tracing-bridge-otel` (ADR 0011) ya inyecta `traceId`/`spanId` en el
patrón de log de Spring Boot en cuanto el tracing está activo (`LANGFUSE_TRACING_ENABLED=true`);
agregar contexto manual solo duplicaría eso sin el flag prendido.

## Alternativas descartadas
- **`logback-spring.xml` con salida JSON** -- descartado por ahora: Railway ya parsea logs de
  texto plano línea por línea en su propio visor, y no hay un sink externo (Datadog, ELK) en el
  alcance de este proyecto que se beneficie de JSON estructurado. Si eso cambia, este ADR queda
  obsoleto.
- **Loguear los cuatro casos de `BookingExceptionHandler` en WARN** -- descartado: son resultados
  de negocio esperables (un usuario cancela una reserva que no es suya, o pide un horario
  ocupado), no fallas del sistema. Loguearlos en WARN en producción ensuciaría el nivel que debería
  reservarse para señales operacionales reales.
- **Incluir el username en el log de login rechazado** -- descartado: permitiría reconstruir desde
  los logs qué cuentas existen a partir de intentos fallidos, el mismo riesgo que ADR 0008 evita en
  la respuesta HTTP.

## Consecuencias
En producción (`LOG_LEVEL` sin fijar, default `INFO`) los logs muestran failover de proveedor,
rate limiting, 503 por LLM no configurado, logins rechazados y cualquier excepción no anticipada
con su stack trace -- sin el ruido de cada reserva rechazada por conflicto de horario. Investigar
un caso puntual de un usuario (por qué le rechazó una cancelación, por qué su token dejó de andar)
requiere subir `LOG_LEVEL=DEBUG` temporalmente, sin redeploy.
