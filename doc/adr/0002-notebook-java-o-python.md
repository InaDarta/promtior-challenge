# 0002. Notebook del demo: Java con rapaio-jupyter-kernel

## Estado
Aceptada (2026-08-27)

## Contexto
El enunciado del challenge da por sentado un notebook en Python, pero el proyecto está en Java 25.
Si el kernel Java de Jupyter no funcionaba contra JDK 25 recién liberado, el plan B era un notebook
en Python que solo llama a la API ya desplegada, con el código Java en celdas de markdown sin
ejecutar. Había que decidir esto temprano (spike [#15](https://github.com/InaDarta/promtior-challenge/issues/15)),
no el último día antes de E09.

## Decisión
Notebook en Java, con `rapaio-jupyter-kernel` (versión 3.0.4).

Se probó de punta a punta en [`notebooks/spike-jupyter-kernel.ipynb`](../../notebooks/spike-jupyter-kernel.ipynb):

- El kernel instala y arranca sin problema contra el JDK 25 del proyecto (con `-preview25`).
- Su magia de dependencias (`%dependency /add` + `/resolve`) resuelve un artefacto real de
  LangChain4j (`langchain4j-google-ai-gemini:1.0.0-beta5`) y sus transitivas desde Maven Central,
  sin necesidad de configurar un `pom.xml` aparte para el notebook.
- Una celda instanció un `GoogleAiGeminiChatModel` y le hizo una llamada real a la API de Gemini,
  que respondió correctamente.

De paso, la llamada de prueba encontró que `gemini-2.5-flash` (el modelo fijado en
[E05](../epics/E05.md)) ya no está disponible para usuarios nuevos; la API redirige a
`gemini-3.6-flash`. Ese modelo, a su vez, ya fue superado por `gemini-3.7-flash` (GA desde el
2026-08-13) para cuando se corrigió esta referencia — el catálogo de Gemini cambia rápido, así
que el modelo vigente se verificó al momento de la corrección en vez de asumir el primer
reemplazo que sugirió el 404. Ese modelo, el que finalmente queda fijado en E05 y en el notebook,
se corrige aparte, no es parte de esta decisión.

## Alternativas descartadas
- **Notebook en Python contra la API desplegada** — era el plan B si el kernel Java fallaba contra
  JDK 25. No hizo falta: el kernel funciona, y un notebook en Java puede mostrar el código real del
  proyecto ejecutándose en vivo (instanciar el `ChatModel`, invocar tools) en vez de solo pegarle a
  un endpoint HTTP con el código fuente citado en markdown.

## Consecuencias
El notebook final (E09.4) puede ejecutar código Java real contra las mismas dependencias del
proyecto, lo que hace la demo más convincente que pegarle a una API ya desplegada. A cambio, quien
corra el notebook necesita el kernel Java instalado además de Jupyter — un paso de setup que un
notebook Python no tendría. La instalación del kernel y sus prerequisitos quedan documentados como
comentario en la primera celda de `notebooks/spike-jupyter-kernel.ipynb`, para reusar en E09.4.
