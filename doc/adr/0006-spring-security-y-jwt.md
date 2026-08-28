# 0006. Spring Security con sesiones sin estado y JWT vía jjwt

## Estado
Aceptada (2026-08-28)

## Contexto
E03.1 pide `POST /api/auth/login` que valide credenciales contra `app_user` (ya sembrada con
BCrypt en E02.2) y devuelva un token, con el resto de la API autenticada salvo login y estáticos.
Ninguna otra parte del sistema todavía depende de la identidad resuelta (los casos de uso de
reservas son E04), así que el diseño solo necesita cubrir login + verificación de token, sin
autorización por rol ni resolución de "usuario actual" para casos de uso -- eso es E03.2/E03.3.

## Decisión
- **Sesiones sin estado (`SessionCreationPolicy.STATELESS`) y JWT como único mecanismo de sesión**:
  no hay `HttpSession` ni cookie; cada request autenticada manda `Authorization: Bearer <token>`.
  Coherente con que el cliente real (el propio LLM ejecutando tools, per el objetivo de la épica
  E03) no es un browser con sesión de formulario.
- **`io.jsonwebtoken:jjwt` (0.12.x) para emitir y parsear el JWT**, HS256 con una clave simétrica
  de `app.jwt.secret` (base64, `JWT_SECRET` en el entorno) -- se descartó Spring Security OAuth2
  Resource Server (pensado para validar tokens de un Authorization Server externo, no para emitirlos
  nosotros mismos) y Nimbus JOSE+JWT directo (misma cobertura que jjwt con una API más verbosa para
  el caso de uso simple de firmar/verificar un HMAC).
  El default en `application.yml` alcanza para dev/tests locales; cualquier despliegue real
  necesita fijar `JWT_SECRET` propio, igual que ya pasa con las credenciales de datasource.
- **`AppUserDetailsService` (implementa `UserDetailsService`) lee `app_user` directo por JPA**, sin
  pasar por un puerto de `application`: resolver credenciales para autenticar es una preocupación
  de framework (Spring Security), no un caso de uso de dominio que la capa `application` necesite
  orquestar. `User` (dominio) sigue siendo solo la identidad dueña de una reserva, sin password.
- **Todo usuario autenticado recibe `ROLE_USER`**: `app_user` no tiene columna de rol y el
  criterio de aceptación de E03.1 no distingue roles -- diferenciar autorización por identidad
  (cancelar reserva ajena) es E03.3, no este issue.
- **`JwtAuthenticationFilter` nunca rechaza la request por token ausente o inválido**: solo puebla
  el `SecurityContext` si el token es válido y deja seguir la cadena; es
  `authorizeHttpRequests` + un `authenticationEntryPoint` explícito (que devuelve 401 en vez del
  403/redirect que da Spring Security por default sin `formLogin`/`httpBasic`) quien decide si el
  endpoint exige autenticación.

## Alternativas descartadas
- **Guardar el usuario autenticado en una `HttpSession`** -- exige sticky sessions o un session
  store compartido para escalar horizontalmente, y no encaja con un cliente no-browser (la tool
  del LLM) que no arrastra cookies entre llamadas.
- **Refresh tokens / rotación** -- fuera de alcance de E03.1; el criterio de aceptación solo pide
  un token válido al loguearse. Se puede sumar después sin romper este diseño (el claim `sub` y el
  filtro no cambian).
- **`UserDetailsService` respaldado por un puerto `application.UserRepository`** -- hubiera sido
  consistente con `BookingRepository`, pero autenticar no es un caso de uso del dominio de reservas;
  meterlo en `application` solo para satisfacer la capa hubiera sido indirección sin beneficio.

## Consecuencias
Cualquier endpoint nuevo queda autenticado por default (whitelist explícita, no blacklist): quien
agregue un controller REST no tiene que acordarse de protegerlo, tiene que acordarse de *no*
protegerlo si de verdad debe ser público. El costo es que un token nunca se puede invalidar antes de
su vencimiento (no hay estado del lado del servidor que revocar) -- aceptable mientras la vigencia
default sea corta (`app.jwt.expiration-minutes`, 60 por defecto) y no haya todavía un caso de uso
que dependa de revocación inmediata.
