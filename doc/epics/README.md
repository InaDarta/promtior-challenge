# Desglose del trabajo

Diez épicas, 43 sub-issues. Cada sub-issue es una unidad del tamaño de un PR: una rama, un pull request, un cambio revisable.

Las épicas conservan su checklist de alcance (87 ítems en total) como detalle fino. Los sub-issues son la unidad de ejecución y están enlazados como sub-issues nativos de GitHub, así que cada épica muestra su progreso sola.

## Convenciones

| Aspecto | Regla |
|---|---|
| Título | `[E0X.N] Descripción en imperativo` |
| Labels | El `area:` de la épica padre más su `prio:`. Nunca `epic` |
| Milestone | El mismo que la épica padre |
| Rama | `feature/E0X.N-slug`, o `chore/` y `docs/` según corresponda |
| PR | Uno por sub-issue, squash merge contra `develop`, con `Closes #N` |

## M1 Core — sistema de reservas correcto y probado, sin IA

### #1 · E00 Base del repositorio y del proceso
- #11 E00.1 Scaffolding Maven con Java 25 y Spring Boot
- #12 E00.2 Calidad de código: Spotless y editorconfig
- #13 E00.3 CI en GitHub Actions y protección de ramas
- #14 E00.4 Plantillas de issue y PR, estructura de ADRs
- #15 E00.5 Spike: kernel Java de Jupyter contra JDK 25

### #2 · E01 Dominio de reservas y sus reglas
- #16 E01.1 Value objects Room, TimeSlot y BookingRange
- #17 E01.2 Entidad Booking e invariantes de creación
- #18 E01.3 Regla de duración máxima de 3 horas
- #19 E01.4 Regla de no solapamiento en la misma sala
- #20 E01.5 Horario de oficina, no-pasado y Clock inyectable
- #21 E01.6 BookingError sellado y cálculo de disponibilidad

### #3 · E02 Persistencia y datos semilla
- #22 E02.1 Esquema Flyway y entidades JPA
- #23 E02.2 Seed de salas y usuarios
- #24 E02.3 Constraint de exclusión y test de concurrencia

### #4 · E03 Autenticación e identidad
- #25 E03.1 Spring Security con login JWT
- #26 E03.2 Resolución del usuario actual
- #27 E03.3 Tests de autorización y suplantación

## M2 Agente — chatbot con tool calling usable punta a punta

### #5 · E04 Casos de uso y API REST
- #28 E04.1 Casos de uso de consulta
- #29 E04.2 Casos de uso de escritura
- #30 E04.3 Endpoints REST con DTOs, validación y OpenAPI
- #31 E04.4 Contrato de error problem+json

### #6 · E05 Agente conversacional y tool calling
- #32 E05.1 Spike: API key de Gemini y límites vigentes
- #33 E05.2 Abstracción de proveedor ChatModel por perfil
- #34 E05.3 BookingAssistant y endpoint de chat
- #35 E05.4 Tools de consulta
- #36 E05.5 Tools de escritura
- #37 E05.6 System prompt y manejo conversacional

### #7 · E06 Interfaz conversacional web
- #38 E06.1 Pantalla de login
- #39 E06.2 Pantalla de chat
- #40 E06.3 Streaming y panel de agenda del día · P2

## M3 Entrega — deploy público, documentación y formulario

### #8 · E07 Testing y evaluación del agente
- #41 E07.1 Suite de integración y tests de API
- #42 E07.2 Test determinista del agente con ChatModel stub
- #43 E07.3 Suite de evaluación en vivo y reporte
- #44 E07.4 Trazas de LLM y tool calls con Langfuse · P2

### #9 · E08 Deploy y operación
- #45 E08.1 Dockerfile multi-stage y perfil prod
- #46 E08.2 Proyecto Railway con Postgres y secretos
- #47 E08.3 Health check y rate limit en /api/chat
- #48 E08.4 Runbook de despliegue y rotación de secretos

### #10 · E09 Documentación y entrega
- #49 E09.1 doc/overview.md con enfoque y desafíos
- #50 E09.2 Diagrama de componentes en Excalidraw
- #51 E09.3 Los cuatro ADRs
- #52 E09.4 Notebook Jupyter
- #53 E09.5 README, GIF de demo y envío del formulario

## Camino crítico

E00 → E01 → E02 → E03 → E04 → E05 → E08 → E09.

E06 y E07 son las únicas épicas recortables. Dentro de ellas, #40 y #44 están marcadas P2 y son lo primero en descartarse.
