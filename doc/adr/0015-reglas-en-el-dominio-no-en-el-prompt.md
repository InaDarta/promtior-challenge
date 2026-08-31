# 0015. Las reglas de negocio viven en el dominio, no en el prompt

## Estado
Aceptada (2026-08-27)

## Contexto
Con un LLM operando el sistema de reservas por tool calling, hay que decidir dónde vive la fuente
de verdad de las reglas de negocio (RN-01 a RN-08: horario de oficina, duración máxima,
contigüidad, solapamiento, capacidad de sala, título obligatorio, entre otras): como invariantes
verificadas en Java dentro de `domain`, o como instrucciones en lenguaje natural dentro del system
prompt, confiando en que el modelo las "recuerde" y las respete turno a turno.

## Decisión
Toda regla de negocio se implementa como invariante de tipos en `domain` — constructores compactos
de `record` y un `sealed interface BookingError` exhaustivo (ver [0001](0001-stack.md)) — paquete
que no importa Spring, JPA ni ninguna dependencia de framework, y se prueba sin levantar contexto.
El system prompt (`BookingSystemPrompt`) describe esas mismas reglas en lenguaje llano, pero solo
para que el modelo pueda explicarlas al usuario y evitar de entrada llamadas obviamente inválidas
— nunca como mecanismo de validación. El prompt deja explícito que el modelo no aplica las reglas,
las aplica el sistema al invocar la tool, y que el modelo debe confirmar con el resultado real de
la tool en vez de asumir que una solicitud "razonable" va a funcionar. Las `@Tool` de
`BookingTools` son adaptadores finos que delegan en los casos de uso de `application`/`domain` sin
lógica propia: el modelo orquesta y explica, el dominio decide.

## Alternativas descartadas
- **Reglas solo en el prompt** — un LLM no es determinístico ni auditable; una alucinación o un
  intento de prompt injection podría llevar a construir una reserva inválida sin que nada lo
  impida a nivel de sistema. El criterio de aceptación de E05 exige explícitamente que una
  violación de regla nunca resulte en una reserva creada, garantía que ningún prompt puede dar por
  sí solo.
- **Reglas duplicadas en el prompt y en el dominio** — descartado por riesgo de divergencia: al
  cambiar una regla (por ejemplo, la duración máxima) haría falta recordar tocar dos lugares, y el
  prompt quedaría desactualizado en silencio, sin que ningún test lo detecte — el dominio sí tiene
  tests, el texto del prompt no.
- **Validar en la capa REST o en las tools en vez de en `domain`** — dejaría el dominio anémico,
  con las reglas como checks dispersos en el adaptador en vez de invariantes del compilador, que es
  la premisa central de [0001](0001-stack.md): una reserva inválida debe ser irrepresentable, no
  solo rechazada en runtime por una capa externa.

## Consecuencias
Una violación de regla de negocio es imposible de colar por más que el modelo se equivoque, alucine
o reciba un prompt adversarial: el dominio la rechaza sin importar por dónde llegue la solicitud.
El prompt queda liviano — solo explica, no valida — pero exige mantenerlo alineado en contenido
(qué reglas existen) con `domain`, aunque no en mecanismo de aplicación; un desalineamiento ahí
produce un modelo que promete algo que el sistema después rechaza, no una reserva inválida.
