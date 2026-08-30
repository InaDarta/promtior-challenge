package com.promtior.booking.infrastructure.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Un span "generation" de Langfuse por cada llamada real al {@code ChatModel} de un proveedor --
 * registrado en cada bean que arma {@link ChatModelConfig}, no en {@link FailoverChatModel}, para
 * que el proveedor de respaldo también quede instrumentado sin que este listener sepa nada de
 * failover. Ver ADR 0011.
 *
 * <p>El span se crea en {@link #onRequest} y se cierra recién en {@link #onResponse}/{@link
 * #onError} para que su duración sea la latencia real del proveedor. Como esos tres métodos pueden
 * correr en hilos distintos en el camino de streaming (la respuesta llega en el hilo del cliente
 * HTTP del proveedor, no en el que abrió la conversación), el span en sí -- no un {@link
 * io.opentelemetry.context.Context}, que sí depende del hilo -- viaja de uno a otro a través del
 * mapa {@code attributes} que LangChain4j comparte entre los tres callbacks de una misma llamada:
 * el padre queda resuelto una sola vez, al crear el span en {@link #onRequest}, con el contexto
 * ambiente de ESE hilo (el original, antes de cualquier despacho asíncrono), y de ahí en más da
 * igual en qué hilo termine la llamada.
 *
 * <p>Cualquier excepción de instrumentación se atrapa y se loguea -- nunca debe romper una
 * conversación real por un bug de tracing.
 */
@Component
class TracingChatModelListener implements ChatModelListener {

  private static final Logger log = LoggerFactory.getLogger(TracingChatModelListener.class);
  private static final String SPAN_ATTRIBUTE = "com.promtior.booking.tracing.span";

  private final Tracer tracer;
  private final LangfuseProperties properties;
  private final ObjectMapper objectMapper;

  TracingChatModelListener(
      Tracer tracer, LangfuseProperties properties, ObjectMapper objectMapper) {
    this.tracer = tracer;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  public void onRequest(ChatModelRequestContext context) {
    if (!properties.enabled()) {
      return;
    }
    try {
      String modelName = context.chatRequest().modelName();
      Span span = tracer.spanBuilder("chat " + modelName).startSpan();
      span.setAttribute("langfuse.observation.type", "generation");
      span.setAttribute("gen_ai.operation.name", "chat");
      span.setAttribute("gen_ai.system", proveedor(context));
      span.setAttribute("gen_ai.request.model", String.valueOf(modelName));
      setJsonAttribute(
          span, "langfuse.observation.input", formatMessages(context.chatRequest().messages()));
      context.attributes().put(SPAN_ATTRIBUTE, span);
    } catch (RuntimeException e) {
      log.warn("No se pudo abrir el span de generación", e);
    }
  }

  @Override
  public void onResponse(ChatModelResponseContext context) {
    if (!properties.enabled()) {
      return;
    }
    Span span = (Span) context.attributes().get(SPAN_ATTRIBUTE);
    if (span == null) {
      return;
    }
    try {
      ChatResponse response = context.chatResponse();
      span.setAttribute("gen_ai.response.model", String.valueOf(response.modelName()));
      TokenUsage usage = response.tokenUsage();
      if (usage != null) {
        if (usage.inputTokenCount() != null) {
          span.setAttribute("gen_ai.usage.input_tokens", usage.inputTokenCount());
        }
        if (usage.outputTokenCount() != null) {
          span.setAttribute("gen_ai.usage.output_tokens", usage.outputTokenCount());
        }
      }
      setJsonAttribute(span, "langfuse.observation.output", formatAiMessage(response.aiMessage()));
    } catch (RuntimeException e) {
      log.warn("No se pudo completar el span de generación", e);
    } finally {
      span.end();
    }
  }

  @Override
  public void onError(ChatModelErrorContext context) {
    if (!properties.enabled()) {
      return;
    }
    Span span = (Span) context.attributes().get(SPAN_ATTRIBUTE);
    if (span == null) {
      return;
    }
    span.recordException(context.error());
    span.setStatus(StatusCode.ERROR, String.valueOf(context.error().getMessage()));
    span.end();
  }

  private static String proveedor(ChatModelRequestContext context) {
    return context.modelProvider() == null
        ? "desconocido"
        : context.modelProvider().name().toLowerCase(Locale.ROOT);
  }

  private void setJsonAttribute(Span span, String key, Object value) {
    try {
      span.setAttribute(key, objectMapper.writeValueAsString(value));
    } catch (RuntimeException | JsonProcessingException e) {
      log.debug("No se pudo serializar {} a JSON para el span", key, e);
    }
  }

  private static List<Map<String, Object>> formatMessages(List<ChatMessage> messages) {
    return messages.stream().map(TracingChatModelListener::formatMessage).toList();
  }

  private static Map<String, Object> formatMessage(ChatMessage message) {
    if (message instanceof SystemMessage system) {
      return Map.of("role", "system", "content", system.text());
    }
    if (message instanceof UserMessage user) {
      return Map.of(
          "role", "user", "content", user.hasSingleText() ? user.singleText() : user.contents());
    }
    if (message instanceof AiMessage ai) {
      return formatAiMessage(ai);
    }
    if (message instanceof ToolExecutionResultMessage toolResult) {
      return Map.of("role", "tool", "tool", toolResult.toolName(), "content", toolResult.text());
    }
    return Map.of("role", message.type().name(), "content", String.valueOf(message));
  }

  private static Map<String, Object> formatAiMessage(AiMessage message) {
    Map<String, Object> formatted = new LinkedHashMap<>();
    formatted.put("role", "assistant");
    if (message.text() != null) {
      formatted.put("content", message.text());
    }
    if (message.hasToolExecutionRequests()) {
      formatted.put(
          "toolCalls",
          message.toolExecutionRequests().stream()
              .map(TracingChatModelListener::formatToolCall)
              .toList());
    }
    return formatted;
  }

  private static Map<String, Object> formatToolCall(ToolExecutionRequest request) {
    return Map.of("name", request.name(), "arguments", request.arguments());
  }
}
