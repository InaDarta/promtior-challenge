package com.promtior.booking.infrastructure.llm.tools;

import com.promtior.booking.infrastructure.llm.config.BookingAssistantConfig;
import com.promtior.booking.infrastructure.llm.tracing.TracingToolExecutor;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Envuelve un {@link ToolExecutor} poblando el {@link SecurityContextHolder} con el {@code
 * memoryId} (el username autenticado, ver {@link BookingAssistantConfig}) antes de ejecutar la tool
 * real.
 *
 * <p>{@code SecurityContextHolder} es thread-local, pero en el camino de streaming la tool corre en
 * el hilo del cliente HTTP del proveedor de LLM, no en el que abrió la conversación (mismo problema
 * que documenta {@link TracingToolExecutor} para el contexto de OTel) -- sin este wrapper,
 * cualquier tool que dependa de {@code CurrentUserProvider} (todas las de escritura) revienta con
 * {@code IllegalStateException} apenas el modelo decide invocarla.
 *
 * <p>El {@code memoryId} es seguro para esto porque {@link BookingAssistantConfig} lo fija al
 * username ya autenticado por {@code SecurityConfig} -- nunca un valor que el modelo complete a
 * partir del texto del chat (ADR 0007).
 */
public class SecurityContextToolExecutor implements ToolExecutor {

  private final ToolExecutor delegate;

  public SecurityContextToolExecutor(ToolExecutor delegate) {
    this.delegate = delegate;
  }

  @Override
  public String execute(ToolExecutionRequest request, Object memoryId) {
    if (!(memoryId instanceof String username)) {
      return delegate.execute(request, memoryId);
    }

    SecurityContext previous = SecurityContextHolder.getContext();
    try {
      SecurityContext toolContext = SecurityContextHolder.createEmptyContext();
      toolContext.setAuthentication(
          new UsernamePasswordAuthenticationToken(username, null, List.of()));
      SecurityContextHolder.setContext(toolContext);
      return delegate.execute(request, memoryId);
    } finally {
      SecurityContextHolder.setContext(previous);
    }
  }
}
