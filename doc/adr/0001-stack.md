# 0001. Stack: Java 25, Spring Boot y LangChain4j

## Estado
Aceptada (2026-08-27)

## Contexto
El enunciado del challenge no fija lenguaje ni framework: solo pide un chatbot con tool calling
que opere un sistema de reservas de salas. Con el lenguaje libre, la elección se juega entre
mostrar dominio con tipado fuerte e inmutable (reglas de negocio como invariantes del compilador,
no como validaciones dispersas) o moverse rápido en el ecosistema donde LangChain nació. También
hay que decidir persistencia: el solapamiento de reservas en la misma sala (RN-07) es una
condición que conviene reforzar a nivel de base de datos, no solo en el dominio.

## Decisión
Java 25 con Spring Boot 3.5 para la aplicación, LangChain4j como capa de tool calling sobre el
LLM, y PostgreSQL como persistencia, desplegado en Railway.

- **Java 25**: LTS más reciente. `record` y `sealed interface` con pattern matching modelan el
  dominio (`Booking`, `BookingError`) sin Lombok ni anotaciones — invariantes verificados por el
  compilador, no por convención.
- **Spring Boot 3.5**: DI, arranque de proyecto y superficie de testing (`spring-boot-starter-test`)
  maduros; no compite con el objetivo del challenge, que es el agente y el dominio, no el
  framework web.
- **LangChain4j**: única librería en el ecosistema JVM con abstracción de `ChatModel` madura y
  multi-proveedor (Gemini, Groq, Ollama) bajo la misma interfaz — necesario para poder cambiar de
  proveedor por perfil de Spring sin tocar código, como exige E05.
- **PostgreSQL**: soporta `EXCLUDE` constraint con GiST sobre rangos de tiempo, que da una segunda
  línea de defensa contra el solapamiento de reservas a nivel de base de datos, además de la regla
  de dominio.
- **Railway**: Postgres administrado y despliegue desde GitHub sin configuración de infraestructura
  aparte.

## Alternativas descartadas
- **Python + FastAPI + LangChain** — es el ecosistema donde LangChain nació y donde más ejemplos
  hay, pero se descarta porque el dominio quedaría validado con convenciones (Pydantic, funciones)
  en vez de con invariantes del tipo — el objetivo es que una reserva inválida sea irrepresentable,
  no solo rechazada en runtime.
- **Node.js/TypeScript + LangChain.js** — tipado estructural y sin `sealed types` reales; el
  modelado de `BookingError` como jerarquía cerrada y exhaustiva es más débil que con Java.
- **Spring AI en vez de LangChain4j** — al momento de decidir, su soporte de tool calling
  multi-proveedor era menos maduro que el de LangChain4j para la combinación Gemini + Groq +
  Ollama que pide E05.
- **Quarkus en vez de Spring Boot** — su ventaja (arranque rápido, imagen nativa) no aporta nada
  a un challenge que no se mide por tiempo de arranque, y suma una curva de aprendizaje que no
  se justifica.
- **H2 o SQLite en vez de PostgreSQL** — sin `EXCLUDE` constraint con GiST, el solapamiento de
  reservas dependería enteramente de la capa de aplicación, sin defensa en profundidad a nivel de
  base de datos.

## Consecuencias
El dominio se prueba sin levantar contexto de Spring y las reglas de negocio son invariantes de
tipos, no checks dispersos. A cambio, se pierde la familiaridad del ecosistema Python/JS con
LangChain: menos ejemplos y una librería (LangChain4j) con comunidad más chica. El uso de
`EXCLUDE` de PostgreSQL ata la persistencia a ese motor específico — aceptable porque no hay
intención de portar a otra base.
