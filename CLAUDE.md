# promtior-challenge

Chatbot con tool calling para reservar salas de reunión. Technical challenge de Promtior.

## Stack

Java 25 · Spring Boot 3.5 · LangChain4j · PostgreSQL · Gemini 2.5 Flash con Groq de respaldo · Railway.

## Arquitectura

Hexagonal, bajo `com.promtior.booking`:

- `domain` — modelo de reservas y sus reglas (entidades, value objects, errores). Java puro, sin
  dependencias de framework: se prueba sin levantar contexto de Spring.
- `application` — casos de uso que orquestan el dominio, y los puertos que necesitan. Depende de
  `domain`; nunca al revés.
- `infrastructure` — adaptadores: REST, persistencia, seguridad y proveedores de LLM. Único lugar
  con configuración de framework, único que implementa los puertos de `application`.

Ver el Javadoc de cada `package-info.java` para el detalle.

## Cómo se trabaja

- `main` siempre desplegable. `develop` es la rama de integración.
- Una rama y un PR por sub-issue. **Todo PR va contra `develop`, nunca contra `main`.**
- Rama: `feature/E0X.N-slug`, o `chore/`/`docs/` según corresponda.
- PR: squash merge contra `develop`, con `Closes #N`.
- Plan completo y convenciones de título/labels/milestone en
  [doc/epics/README.md](doc/epics/README.md). Progreso en [doc/PROGRESO.md](doc/PROGRESO.md).

## Build y verificación

El wrapper (`./mvnw`) fija Maven 3.9.16, pero necesita el JDK 25 en `JAVA_HOME` — el `JAVA_HOME`
del entorno suele apuntar a un JDK viejo (8 u otro). Antes de cualquier `./mvnw`:

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-25.0.4.101-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"
```

Comandos habituales:

```bash
./mvnw verify          # build + tests + spotless:check
./mvnw spotless:apply  # reformatea automáticamente
```

### Calidad de código

Spotless (`spotless-maven-plugin`) con `google-java-format`, estilo `GOOGLE` (2 espacios, 100
columnas). `spotless:check` está enganchado a la fase `verify`: un archivo mal formateado hace
fallar `./mvnw verify`, y `./mvnw spotless:apply` lo corrige. El `.editorconfig` en la raíz refleja
la misma configuración (2 espacios, LF, UTF-8, 100 columnas en `.java`).

## Entorno de este equipo

`Selector.open()` de NIO falla dentro de la sandbox de la tool de Claude Code (no es un bug del
código: en la PowerShell normal del usuario funciona bien).
