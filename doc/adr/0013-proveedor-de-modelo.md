# 0013. Proveedor de modelo: Gemini primario, Groq de respaldo

## Estado
Aceptada (2026-08-27)

## Contexto
El proyecto se juega enteramente en tool calling: un LLM que decide qué tool invocar y con qué
argumentos, nunca reglas de negocio por su cuenta (ver [0015](0015-reglas-en-el-dominio-no-en-el-prompt.md)).
Hay que elegir proveedor de modelo primario y un criterio de fallback, con estas restricciones:
sin tarjeta de crédito (el challenge no tiene presupuesto asignado), y con soporte de tool calling
lo bastante confiable para que el agente no falle por el modelo en sí, sino a lo sumo por una regla
de negocio real. LangChain4j ya fue elegido en [0001](0001-stack.md) por exponer varios proveedores
bajo la misma interfaz `ChatModel`, así que la decisión acá es de candidatos, no de mecanismo.

## Decisión
Gemini (`gemini-3.7-flash` vía `langchain4j-google-ai-gemini`) como proveedor primario, con Groq
(`llama-3.3-70b-versatile` vía cliente OpenAI-compatible) como fallback documentado, y Ollama para
desarrollo offline. Los tres detrás del mismo bean `ChatModel`, uno por perfil de Spring (E05.2) —
cambiar de proveedor es configuración, no código. El fallback automático ante `503` o agotamiento
de cuota se implementa en E05.2, después de que el spike de [0009](0009-limites-del-tier-gratuito-de-gemini.md)
confirmó que el tier gratuito de Gemini es más ajustado de lo esperado.

Criterio: sobre el eje que se evalúa (tool calling), Gemini Flash es más confiable que los modelos
abiertos de Groq. Groq se mantiene como respaldo porque el propio enunciado del challenge lo
sugiere como proveedor, y porque su latencia baja hace que la demo se sienta instantánea cuando
entra en juego.

## Alternativas descartadas
- **Anthropic (Claude) como proveedor** — una suscripción de Claude (Pro o Max) cubre claude.ai y
  Claude Code, pero **no** habilita la API: esa se factura aparte por consumo desde
  console.anthropic.com. Incompatible con la restricción de "sin tarjeta" que sí cumplen el free
  tier de Google AI Studio y el de Groq.
- **OpenRouter como proveedor primario** — los modelos marcados `:free` tienen límites de uso
  agresivos, y el soporte de tool calling depende del modelo subyacente que enrutan en cada
  momento, no es una garantía uniforme de la plataforma. Queda como tercera red de contención, no
  como primario ni como fallback documentado.
- **Un solo proveedor sin fallback** — descartado antes de conocer los números exactos del spike
  de 0009: un challenge que se demuestra en vivo no puede depender de un único punto de falla
  frente a saturación (`503`) o cuota agotada de un solo proveedor.

## Consecuencias
El agente queda desacoplado de un proveedor específico: un cambio de perfil de Spring alcanza para
correr contra Gemini, Groq u Ollama sin tocar código de `application` ni `domain`. A cambio, el
proyecto sostiene tres integraciones de proveedor (tres SDKs, tres formatos de error) en vez de
una, y el comportamiento del agente puede variar sutilmente según qué proveedor esté activo en un
momento dado — un riesgo que E05.2 mitiga con tests de tool calling que corren contra los tres
perfiles.
