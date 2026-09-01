# 0007. Resolución del usuario actual vía un puerto de `application`

## Estado
Aceptada (2026-08-28)

## Contexto
E03.2 pide que los casos de uso sepan quién es el usuario autenticado sin que esa identidad viaje
nunca como parámetro de entrada (ni en el body de la API, ni en el argumento de una tool). En #6
las tools las invoca un LLM a partir de texto libre del usuario; si la identidad fuera un argumento
más, un prompt como «reservá esto a nombre de User2» alcanzaría para suplantarlo. `JwtAuthenticationFilter`
(ADR 0006) ya puebla el `SecurityContextHolder` con la identidad del token en cada request -- falta
un único punto donde un caso de uso la lea, sin acoplarse a Spring Security para hacerlo.

## Decisión
- **Puerto `application.CurrentUserProvider`** con un solo método, `currentUser(): User` (dominio).
  Los casos de uso dependen de esta interfaz, nunca de `Authentication` ni de
  `SecurityContextHolder` -- misma regla de dependencia que `BookingRepository`: `application` no
  conoce el framework que hay detrás del puerto.
- **`infrastructure.security.SecurityContextCurrentUserProvider`** es la única clase que lee
  `SecurityContextHolder.getContext().getAuthentication()`. Traduce `authentication.getName()`
  (el username, vía `UserDetails`) a un `User` de dominio. Si no hay autenticación en el contexto
  lanza `IllegalStateException`: no debería ocurrir en ningún endpoint protegido por
  `SecurityConfig`, así que fallar ruidosamente ahí es preferible a devolver un usuario nulo o
  inventado.
- **Ningún caso de uso ni controller recibe el usuario como argumento desde la capa web**: la
  identidad se obtiene siempre invocando el puerto, nunca deserializando un campo del request. Esto
  hace que la garantía sea estructural (no hay parámetro que un LLM pueda rellenar con otro
  nombre), no una convención que dependa de que cada handler la respete.

## Alternativas descartadas
- **Que el controller extraiga el `Authentication` (o el username) y lo pase como argumento al caso
  de uso** -- funciona igual en el camino feliz, pero reintroduce la identidad como un parámetro
  más en la firma del caso de uso: exactamente el vector que la épica busca eliminar. Si mañana una
  tool del LLM invoca el mismo caso de uso, la firma ya "acepta" un usuario y nada impide que
  alguien la rellene con el texto del chat.
- **Que los casos de uso dependan directo de `SecurityContextHolder`/`Authentication`** -- acopla
  `application` a clases de Spring Security, violando la regla de dependencia hexagonal (Ver
  Javadoc de `application/package-info.java`: depende de `domain`, nunca de un framework).
- **Bean de scope `request` que resuelve el usuario una vez y lo cachea** -- complejidad extra sin
  beneficio: `SecurityContextHolder` ya es thread-local por request (lo puebla el filtro antes de
  que el dispatcher llegue al controller), así que leerlo en cada llamada al puerto es tan barato
  como cachearlo.

## Consecuencias
Cualquier caso de uso de reservas (E04) que necesite saber "quién soy" inyecta
`CurrentUserProvider` en vez de agregar un parámetro `User` a su método -- si alguien lo hiciera de
todos modos, un test de suplantación (E03.3) que llame al endpoint pidiendo actuar "a nombre de
User2" lo expondría igual, porque el valor que ese parámetro traería nunca es el que el caso de uso
termina usando. La única complejidad nueva es que cualquier código que llame a `currentUser()` fuera
de un request autenticado (un test unitario sin `SecurityContext`, un job en background) tiene que
poblar el contexto explícitamente o recibir la excepción.
