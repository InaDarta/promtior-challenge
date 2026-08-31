# Runbook de despliegue y rotación de secretos

Para quien tenga que desplegar, revertir o rotar un secreto sin haber participado del
desarrollo. Cubre Railway (`main` → producción). No asume acceso previo al proyecto de
Railway ni conocimiento del código.

## 1. Variables de entorno

Todas se configuran como variables de entorno del servicio en Railway — nunca como archivo
versionado. `src/main/resources/application.yml` fija los defaults de dev/test;
`application-prod.yml` (perfil `prod`, activado por el `Dockerfile`) los pisa y, para las
marcadas **sin default** abajo, no tiene ninguno propio: si falta alguna, el proceso no
arranca.

### Imprescindibles para cualquier despliegue real

| Variable | Qué hace | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC de Postgres | sin default en `prod` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base | sin default en `prod` |
| `SPRING_DATASOURCE_PASSWORD` | Password de la base | sin default en `prod` |
| `JWT_SECRET` | Clave HS256 (base64, ≥256 bits) para firmar los tokens de login. El default de `application.yml` es fijo y solo sirve para dev/tests — **nunca usarlo en un despliegue real** (ver ADR 0006) | sin default en `prod` |
| `SPRING_PROFILES_ACTIVE` | Ver advertencia abajo | `prod` (fijado en el `Dockerfile`) |

> ⚠️ **El perfil `prod` no alcanza solo.** El proveedor de LLM (Gemini, Groq u Ollama) se
> activa con un perfil de Spring aparte (`gemini`, `groq` u `ollama` — ver
> `ChatModelConfig`), no con variables sueltas. El `Dockerfile` solo fija
> `SPRING_PROFILES_ACTIVE=prod`; si en Railway no se agrega el perfil de proveedor
> (`SPRING_PROFILES_ACTIVE=prod,gemini` para el caso normal), la app arranca y
> `/actuator/health` da `UP`, pero **todo mensaje al chat devuelve 503** porque no hay
> ningún `ChatModel` en el contexto (`LlmNotConfiguredException`). Es el error más fácil de
> cometer al configurar el servicio por primera vez — ver también §5.

### Proveedor de LLM (con el perfil `gemini` activo, el caso normal)

| Variable | Qué hace | Default |
|---|---|---|
| `GEMINI_API_KEY` | API key de Google AI Studio, proveedor primario | vacío (sin key, el proveedor falla al primer request) |
| `GEMINI_MODEL_NAME` | Modelo de Gemini | `gemini-3.7-flash` |
| `GEMINI_MAX_RETRIES` | Reintentos del cliente antes de darse por vencido. A propósito bajo — ADR 0009: cada reintento fallido consume cupo de un RPD de 20 | `2` |
| `GROQ_API_KEY` | API key de Groq, proveedor de respaldo (failover automático, ver §5) | vacío |
| `GROQ_MODEL_NAME` | Modelo de Groq | `openai/gpt-oss-20b` |
| `GROQ_BASE_URL` | Endpoint de la API de Groq | `https://api.groq.com/openai/v1` |

Con el perfil `groq` en vez de `gemini`, Groq queda forzado como único proveedor (sin
failover, sin Gemini). `ollama` es solo para desarrollo local offline y no aplica a un
despliegue real.

### Opcionales

| Variable | Qué hace | Default |
|---|---|---|
| `JWT_EXPIRATION_MINUTES` | Vigencia del token de login | `60` |
| `LANGFUSE_TRACING_ENABLED` | Prende el trazado de LLM/tool calls a Langfuse (ADR 0011) | `false` |
| `LANGFUSE_OTEL_ENDPOINT` | Endpoint OTLP de Langfuse | `https://cloud.langfuse.com/api/public/otel/v1/traces` |
| `LANGFUSE_OTEL_AUTH_HEADER` | `Authorization: Basic <base64 de pk:sk>` **ya calculado** — nunca las keys sueltas | vacío |
| `PORT` | Puerto HTTP. Railway lo inyecta solo | `8080` |

## 2. Desplegar

### 2.1 Primera vez

1. Crear un proyecto en Railway y agregar un servicio Postgres (addon nativo de Railway).
2. Crear un segundo servicio apuntando a este repositorio, rama `main`, build con el
   `Dockerfile` de la raíz (Railway lo detecta solo).
3. Cargar las variables de §1 en el servicio de la app:
   - Las de datasource, apuntando al Postgres del paso 1 (Railway permite referenciar
     variables de otro servicio del mismo proyecto, p. ej. `${{Postgres.PGHOST}}`, en vez
     de copiar el valor a mano — así una rotación de credenciales del lado de Postgres se
     propaga sin editarlas de nuevo).
   - `JWT_SECRET` propio (ver §4.3 para cómo generarlo).
   - `GEMINI_API_KEY` y `GROQ_API_KEY`.
   - `SPRING_PROFILES_ACTIVE=prod,gemini` (no solo `prod` — ver advertencia de §1).
4. Activar "Deploy on push" para `main` (auto-deploy; ver §2.2).
5. Disparar el primer deploy y seguir §2.3 para confirmarlo.

No hace falta ninguna migración manual: Flyway corre sola al arrancar (`spring.flyway.enabled:
true`) y aplica el esquema y el seed de salas/usuarios (`V2__seed_salas_usuarios.sql`) contra
la base vacía del addon.

### 2.2 Deploys posteriores

Con "Deploy on push" activo, cada merge a `main` dispara un build y deploy solo. No hay
ningún paso manual ni workflow de GitHub Actions que despliegue — `.github/workflows/ci.yml`
solo corre tests en los PRs; el deploy es enteramente del lado de Railway.

### 2.3 Verificar que el deploy funcionó

1. `GET https://<url-de-railway>/actuator/health` — público, sin token (ver ADR 0012).
   `"status":"UP"` con `components.db.status:"UP"` confirma que la app levantó y que
   Postgres responde. Esto **no** confirma que el proveedor de LLM esté bien configurado
   (ver advertencia de §1) — para eso hace falta un login + mensaje de chat real.
2. Loguearse con las credenciales del enunciado (usuarios `User1`/`User2`, sembrados por
   `V2__seed_salas_usuarios.sql` — la contraseña es la del enunciado del challenge, no vive
   en texto plano en ningún archivo del repo) y mandar un mensaje al chat que dispare al
   menos una tool call (p. ej. "¿qué salas están libres mañana a las 10?").
3. Revisar los logs del deploy en Railway durante el arranque en frío: desde que arranca el
   contenedor hasta el primer `/actuator/health` en `UP` es el tiempo de arranque en frío a
   documentar/monitorear (JVM + Flyway + Spring context).

## 3. Revertir un despliegue

Dos formas, de más simple a más segura:

- **Desde Railway**: cada deploy queda listado con su commit; "Redeploy" sobre un deploy
  anterior vuelve a esa imagen sin tocar `main`. Es la vía más rápida para cortar una
  demo rota en el momento.
- **Desde git**: revertir el merge en `main` (`git revert`) y pushearlo — dispara un deploy
  nuevo con el código anterior. Preferible si el problema va a tardar en arreglarse, porque
  deja `main` consistente con lo que está desplegado (recordar: "`main` siempre
  desplegable").

Revertir el código no revierte una migración de Flyway ya aplicada ni el estado de la base.
Si el problema es de datos (no de código), no alcanza con este paso.

## 4. Rotar secretos

Ninguna rotación necesita downtime planeado más allá del reinicio normal del redeploy
(segundos).

### 4.1 `GEMINI_API_KEY` / `GROQ_API_KEY`

1. Generar la key nueva en el panel del proveedor (Google AI Studio / Groq console).
2. Actualizar la variable en Railway y guardar — Railway redespliega el servicio solo.
3. Confirmar con un mensaje de chat real (§2.3, paso 2).
4. Revocar la key vieja en el panel del proveedor.

### 4.2 `JWT_SECRET`

1. Generar 256+ bits al azar y codificarlos en base64, p. ej.:
   ```bash
   openssl rand -base64 32
   ```
2. Actualizar `JWT_SECRET` en Railway.
3. **Consecuencia a tener en cuenta**: no hay revocación de tokens del lado del servidor
   (ADR 0006) — rotar la clave invalida de golpe *todos* los tokens ya emitidos, válidos o
   no. Cualquiera con sesión abierta tiene que loguearse de nuevo. Para una demo, avisar
   antes de rotar en medio de una sesión activa.

### 4.3 Credenciales de la base de datos

1. Regenerar el usuario/password del lado del addon de Postgres en Railway (o
   `ALTER USER ... PASSWORD '...'` conectándose directo si se administra la credencial a
   mano).
2. Si `SPRING_DATASOURCE_USERNAME`/`PASSWORD`/`URL` están seteadas como referencia a las
   variables del servicio Postgres (recomendado, ver §2.1), se actualizan solas al
   redesplegar. Si están copiadas a mano, actualizarlas ahí también.
3. Verificar `/actuator/health` → `components.db.status:"UP"` después del redeploy.

### 4.4 `LANGFUSE_OTEL_AUTH_HEADER`

Regenerar el par `pk`/`sk` en Langfuse, recalcular el header como `Basic <base64 de
pk:sk>` (no las keys sueltas) y actualizar la variable en Railway. Si `LANGFUSE_TRACING_ENABLED`
está en `false`, esta rotación no es urgente — nada se exporta.

## 5. Qué mirar cuando la demo falla

| Síntoma | Dónde mirar | Qué significa |
|---|---|---|
| El chat devuelve 503 en **todo** mensaje, `/actuator/health` en `UP` | `SPRING_PROFILES_ACTIVE` del servicio en Railway | Falta el perfil de proveedor (`gemini`/`groq`/`ollama`) — ver advertencia de §1. `LlmNotConfiguredException` no deja rastro en los logs (no tiene logging propio) |
| `/actuator/health` en `DOWN`, `components.db.status:"DOWN"` | Estado del addon de Postgres en Railway | Base caída o credenciales desactualizadas — ver §4.3 |
| Chat funciona pero tarda mucho o falla intermitente | Logs de la app, buscar `"Proveedor primario no disponible, usando el de respaldo"` (`FailoverChatModel`/`FailoverStreamingChatModel`) | Gemini devolvió un error transitorio (503, 429, 5xx o problema de red) y el sistema ya conmutó solo a Groq — ADR 0009. Si aparece seguido, Gemini está saturado o sin cupo, no es un bug |
| Chat devuelve 429 con `Retry-After` | La respuesta HTTP misma — **no queda logueado del lado del servidor**, `ChatRateLimitFilter` no loguea nada | Se agotó el cupo global (5/min) o el del usuario (2/min) — ADR 0012. Es el comportamiento esperado protegiendo la cuota de Gemini, no una falla. El mensaje distingue cupo global ("El asistente alcanzó su límite...") de cupo por usuario ("Estás enviando mensajes muy rápido...") |
| Cuota de Gemini agotada para el día (RPD=20) | [aistudio.google.com/rate-limit](https://aistudio.google.com/rate-limit) con la cuenta de la API key | Confirmado por ADR 0009: los intentos fallidos con 503 también consumen RPD. El failover a Groq ya cubre esto en tiempo real; para el día siguiente el cupo se resetea solo |
| 401 en cualquier request autenticada | Vigencia del JWT (`JWT_EXPIRATION_MINUTES`, default 60) o si se rotó `JWT_SECRET` recientemente (§4.2) | Token vencido o invalidado por rotación de clave — pedirle al usuario que vuelva a loguearse |

Los logs del proceso en sí se ven desde el panel de deploy/servicio en Railway (no hay
agregador externo configurado). Para trazas detalladas de cada llamada al LLM y cada tool
call, con `LANGFUSE_TRACING_ENABLED=true` quedan en el dashboard de Langfuse (ADR 0011).
