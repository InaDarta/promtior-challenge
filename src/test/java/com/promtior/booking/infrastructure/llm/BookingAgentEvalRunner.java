package com.promtior.booking.infrastructure.llm;

import com.promtior.booking.application.CancelBooking;
import com.promtior.booking.application.CreateBooking;
import com.promtior.booking.application.GetRoomSchedule;
import com.promtior.booking.application.ListAvailableRooms;
import com.promtior.booking.application.ListMyBookings;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Runner de la suite de evaluación en vivo de E07.3 (issue #43): corre {@link EvalDataset#casos()}
 * contra un {@link ChatModel} REAL -- Gemini o Groq, según qué API key esté en el entorno -- y
 * agrega una sección con el resultado, la fecha y el modelo de la corrida a {@code
 * doc/eval/E07.3-resultados.md}, sin pisar corridas anteriores (así queda una tabla por proveedor
 * para poder comparar, que es el criterio de aceptación del issue).
 *
 * <p>A diferencia de {@link ChatModelConfig} en producción, esta suite corre un único proveedor por
 * vez y sin el {@link FailoverChatModel} -- correr con failover mezclaría dos modelos bajo un solo
 * nombre en el reporte, justo lo que el issue pide poder distinguir.
 *
 * <p><b>Nunca corre en CI</b>: a propósito no se llama {@code *Test}/{@code *Tests}/{@code
 * *TestCase} -- los patrones de inclusión por default de Surefire no lo enganchan, así que {@code
 * ./mvnw verify} (lo que corre {@code .github/workflows/ci.yml}) nunca lo ejecuta ni consume cuota
 * de ninguna API key. Para correrlo a mano:
 *
 * <pre>{@code
 * export JAVA_HOME=".../jdk-25..."
 * export GEMINI_API_KEY=...       # o GROQ_API_KEY -- con las dos puestas, gana Gemini
 * ./mvnw test -Dtest=BookingAgentEvalRunner
 * }</pre>
 *
 * <p>Sin ninguna de las dos keys, el único {@code @Test} se saltea (no falla) vía {@link
 * Assumptions#assumeTrue}. El tier gratuito de Gemini tiene RPD=20 (ADR 0009): correr el dataset
 * completo (~20 frases, varias de dos turnos) contra Gemini en una sola sesión puede agotar el cupo
 * diario antes de terminar. {@code EVAL_CASOS} (ids separados por coma, ej. {@code
 * "C1,C6,C12,C17"}) permite correr un subconjunto -- el "alcance recortado" de 8 frases que el
 * issue explícitamente habilita.
 */
class BookingAgentEvalRunner {

  private static final Path REPORTE = Path.of("doc", "eval", "E07.3-resultados.md");

  @Test
  void correrSuiteDeEvaluacionEnVivo() throws IOException {
    Proveedor proveedor = detectarProveedor();
    Assumptions.assumeTrue(
        proveedor != null,
        "Ninguna de GEMINI_API_KEY / GROQ_API_KEY está en el entorno: la suite de evaluación en"
            + " vivo se saltea, no corre contra ningún ChatModel real. Ver el Javadoc de"
            + " BookingAgentEvalRunner para cómo correrla a mano.");

    List<EvalCase> casos = seleccionarCasos();
    System.out.printf(
        "Corriendo %d caso(s) de la suite de evaluación contra %s (%s)...%n",
        casos.size(), proveedor.nombre(), proveedor.modelo());

    List<Resultado> resultados = new ArrayList<>();
    for (EvalCase caso : casos) {
      Resultado resultado = correrCaso(caso, proveedor.chatModel());
      resultados.add(resultado);
      System.out.printf(
          "  %s [%s] tools=%s respuesta=%s%n",
          caso.id(),
          resultado.acierto() ? "OK" : "REVISAR",
          resumirToolCalls(resultado.toolCalls()),
          truncar(resultado.respuestaFinal(), 200));
    }

    escribirReporte(proveedor, resultados);
  }

  private static List<EvalCase> seleccionarCasos() {
    String filtro = System.getenv("EVAL_CASOS");
    List<EvalCase> todos = EvalDataset.casos();
    if (filtro == null || filtro.isBlank()) {
      return todos;
    }
    Set<String> idsPedidos =
        Set.of(filtro.split(",")).stream().map(String::strip).collect(Collectors.toSet());
    List<EvalCase> filtrados = todos.stream().filter(c -> idsPedidos.contains(c.id())).toList();
    if (filtrados.isEmpty()) {
      throw new IllegalArgumentException(
          "EVAL_CASOS='%s' no matchea ningún id conocido (son C1..C20)".formatted(filtro));
    }
    return filtrados;
  }

  private Resultado correrCaso(EvalCase caso, ChatModel modeloReal) {
    InMemoryBookingRepository repository = new InMemoryBookingRepository();
    EvalContext contexto = new EvalContext();
    caso.setup().accept(repository, contexto);

    FakeCurrentUserProvider currentUser = new FakeCurrentUserProvider(EvalDataset.YO);
    RecordingChatModel grabador = new RecordingChatModel(modeloReal);
    BookingAssistant assistant = construirAssistant(grabador, repository, currentUser);

    String sessionId = "eval-" + caso.id();
    String ultimaRespuesta = "";
    int marcaUltimoTurno = 0;
    try {
      for (Function<EvalContext, String> turno : caso.turnos()) {
        marcaUltimoTurno = grabador.marca();
        String mensaje = turno.apply(contexto);
        ultimaRespuesta = assistant.chat(sessionId, mensaje);
      }
    } catch (RuntimeException e) {
      // getClass().getName() + printStackTrace(): un mensaje suelto como "Not implemented" no
      // alcanza para diagnosticar nada -- con la excepción completa a la vista se puede saber si
      // es la key, el rate limit o un tipo de parámetro de una tool que el proveedor no soporta.
      System.err.println("Error real en " + caso.id() + ":");
      e.printStackTrace();
      return new Resultado(
          caso,
          List.of(),
          "ERROR (%s): %s".formatted(e.getClass().getName(), e.getMessage()),
          false);
    }

    List<ToolExecutionRequest> toolCalls = grabador.desde(marcaUltimoTurno);
    boolean acierto = calcularAcierto(caso.toolsEsperadas(), toolCalls);
    return new Resultado(caso, toolCalls, ultimaRespuesta, acierto);
  }

  private static boolean calcularAcierto(
      List<String> toolsEsperadas, List<ToolExecutionRequest> toolCalls) {
    if (toolsEsperadas.isEmpty()) {
      return toolCalls.isEmpty();
    }
    return toolCalls.stream().anyMatch(call -> toolsEsperadas.contains(call.name()));
  }

  private static BookingAssistant construirAssistant(
      ChatModel modelo, InMemoryBookingRepository repository, FakeCurrentUserProvider currentUser) {
    Clock clock = EvalDataset.AHORA;
    RoomQueryTools roomQueryTools =
        new RoomQueryTools(new ListAvailableRooms(repository), new GetRoomSchedule(repository));
    BookingQueryTools bookingQueryTools =
        new BookingQueryTools(new ListMyBookings(repository, currentUser));
    BookingTools bookingTools =
        new BookingTools(
            new CreateBooking(repository, currentUser, clock),
            new CancelBooking(repository, currentUser));
    BookingSystemPrompt systemPrompt = new BookingSystemPrompt(clock, currentUser);

    return AiServices.builder(BookingAssistant.class)
        .chatModel(modelo)
        .streamingChatModel(
            new StubStreamingChatModel(
                (request, handler) ->
                    handler.onError(
                        new UnsupportedOperationException("no usado por la suite de evaluación"))))
        .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
        .systemMessageProvider(systemPrompt)
        .tools(roomQueryTools, bookingQueryTools, bookingTools)
        .build();
  }

  private static Proveedor detectarProveedor() {
    String gemini = System.getenv("GEMINI_API_KEY");
    String groq = System.getenv("GROQ_API_KEY");
    if (gemini != null && !gemini.isBlank()) {
      String modelo = envO("GEMINI_MODEL_NAME", "gemini-3.7-flash");
      ChatModel chatModel =
          GoogleAiGeminiChatModel.builder()
              .apiKey(gemini)
              .modelName(modelo)
              .maxRetries(Integer.parseInt(envO("GEMINI_MAX_RETRIES", "2")))
              .build();
      return new Proveedor("gemini", modelo, chatModel);
    }
    if (groq != null && !groq.isBlank()) {
      String modelo = envO("GROQ_MODEL_NAME", "llama-3.3-70b-versatile");
      ChatModel chatModel =
          OpenAiChatModel.builder()
              .baseUrl(envO("GROQ_BASE_URL", "https://api.groq.com/openai/v1"))
              .apiKey(groq)
              .modelName(modelo)
              .build();
      return new Proveedor("groq", modelo, chatModel);
    }
    return null;
  }

  private static String envO(String variable, String porDefecto) {
    String valor = System.getenv(variable);
    return (valor == null || valor.isBlank()) ? porDefecto : valor;
  }

  private static String resumirToolCalls(List<ToolExecutionRequest> toolCalls) {
    if (toolCalls.isEmpty()) {
      return "(ninguna tool)";
    }
    return toolCalls.stream()
        .map(call -> "%s(%s)".formatted(call.name(), call.arguments()))
        .collect(Collectors.joining("; "));
  }

  private void escribirReporte(Proveedor proveedor, List<Resultado> resultados) throws IOException {
    if (!Files.exists(REPORTE)) {
      Files.createDirectories(REPORTE.getParent());
      Files.writeString(REPORTE, ENCABEZADO);
    }
    Files.writeString(REPORTE, seccionDeCorrida(proveedor, resultados), StandardOpenOption.APPEND);
    System.out.println("Reporte actualizado: " + REPORTE.toAbsolutePath());
  }

  private static String seccionDeCorrida(Proveedor proveedor, List<Resultado> resultados) {
    long aciertos = resultados.stream().filter(Resultado::acierto).count();
    StringBuilder sb = new StringBuilder();
    sb.append("\n## Corrida ").append(LocalDate.now()).append('\n');
    sb.append("\n- **Proveedor:** `").append(proveedor.nombre()).append("`\n");
    sb.append("- **Modelo:** `").append(proveedor.modelo()).append("`\n");
    sb.append("- **Casos corridos:** ").append(resultados.size()).append(" de 20\n");
    sb.append("- **Tasa de acierto (tool esperada en el último turno):** ")
        .append(aciertos)
        .append('/')
        .append(resultados.size())
        .append('\n');
    sb.append(
        "\n| # | Categoría | Turno(s) | Tool esperada | Tool call(s) obtenidos | Respuesta final /"
            + " error | ¿Acierto? | Criterio de acierto / notas |\n");
    sb.append("|---|---|---|---|---|---|---|---|\n");
    for (Resultado r : resultados) {
      EvalCase c = r.caso();
      sb.append("| ")
          .append(c.id())
          .append(" | ")
          .append(c.categoria())
          .append(" | ")
          .append(escapar(turnosComoTexto(c)))
          .append(" | ")
          .append(
              c.toolsEsperadas().isEmpty() ? "(ninguna)" : String.join(" o ", c.toolsEsperadas()))
          .append(" | ")
          .append(escapar(resumirToolCalls(r.toolCalls())))
          .append(" | ")
          .append(escapar(truncar(r.respuestaFinal(), 300)))
          .append(" | ")
          .append(r.acierto() ? "✅" : "⚠️")
          .append(" | ")
          .append(escapar(c.criterioDeAcierto()))
          .append(" |\n");
    }
    return sb.toString();
  }

  private static String turnosComoTexto(EvalCase caso) {
    // Solo para el reporte: los turnos que dependen de un id sembrado (EvalContext) no se pueden
    // reconstruir sin correr el caso -- se listan como placeholders reconocibles en vez de fallar.
    List<String> turnos = new ArrayList<>();
    for (Function<EvalContext, String> turno : caso.turnos()) {
      try {
        turnos.add(turno.apply(new EvalContext()));
      } catch (RuntimeException e) {
        turnos.add("(frase con id sembrado en tiempo de ejecución -- ver criterio de acierto)");
      }
    }
    return String.join(" » ", turnos);
  }

  private static String escapar(String texto) {
    return texto.replace("|", "\\|").replace("\n", " ");
  }

  /**
   * Un {@code ERROR: ...} atrapado en {@link #correrCaso} y un "el modelo respondió sin llamar a
   * ninguna tool" legítimo se ven idénticos en {@link #resumirToolCalls} (las dos dan "(ninguna
   * tool)") -- este texto es lo único que distingue una falla real (key inválida, rate limit, un
   * error de red) de un acierto/desacierto genuino del modelo, así que se imprime y se documenta
   * siempre, nunca solo en el caso de error.
   */
  private static String truncar(String texto, int maxCaracteres) {
    if (texto == null) {
      return "";
    }
    String unaLinea = texto.replace("\n", " ⏎ ");
    return unaLinea.length() <= maxCaracteres
        ? unaLinea
        : unaLinea.substring(0, maxCaracteres) + "…";
  }

  private record Proveedor(String nombre, String modelo, ChatModel chatModel) {}

  private record Resultado(
      EvalCase caso,
      List<ToolExecutionRequest> toolCalls,
      String respuestaFinal,
      boolean acierto) {}

  private static final String ENCABEZADO =
      """
      # E07.3 — Suite de evaluación en vivo del agente

      Reporte de [issue #43](https://github.com/InaDarta/promtior-challenge/issues/43): dataset de
      `EvalDataset` corrido contra un `ChatModel` real, no un stub (a diferencia del test determinista
      de E07.2). Cada corrida agrega una sección nueva más abajo, con su fecha y su proveedor/modelo --
      así se puede comparar la tasa de acierto entre proveedores sin perder corridas anteriores.

      Generado por `BookingAgentEvalRunner`. No corre en CI ni consume cuota de ninguna API key salvo
      que se invoque así, a mano:

      ```bash
      export GEMINI_API_KEY=...          # o GROQ_API_KEY
      ./mvnw test -Dtest=BookingAgentEvalRunner
      ```

      Sin ninguna de las dos keys en el entorno, el test se saltea (skip), no falla. El tier gratuito
      de Gemini tiene RPD=20 (ver
      [ADR 0009](../adr/0009-limites-del-tier-gratuito-de-gemini.md)): correr el dataset completo (20
      frases, varias de dos turnos) contra Gemini en una sola sesión puede agotar el cupo diario antes
      de terminar. `EVAL_CASOS` (ids separados por coma, ej.
      `EVAL_CASOS=C1,C6,C9,C12,C13,C17,C18,C20`) corre solo un subconjunto -- el "alcance recortado" de
      8 frases que el issue #43 habilita explícitamente.
      """;
}
