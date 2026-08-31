package com.promtior.booking.infrastructure.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextToolExecutorTest {

  @Test
  void elDelegateVeElUsuarioAutenticadoAunEnOtroHilo() throws Exception {
    ToolExecutionRequest request =
        ToolExecutionRequest.builder().name("cualquiera").arguments("{}").build();
    String[] usernameVistoPorElDelegate = new String[1];
    ToolExecutor delegate =
        (req, memoryId) -> {
          usernameVistoPorElDelegate[0] =
              SecurityContextHolder.getContext().getAuthentication() == null
                  ? null
                  : SecurityContextHolder.getContext().getAuthentication().getName();
          return "ok";
        };
    ToolExecutor wrapped = new SecurityContextToolExecutor(delegate);

    ExecutorService otroHilo = Executors.newSingleThreadExecutor();
    try {
      CompletableFuture<Void> future =
          CompletableFuture.runAsync(() -> wrapped.execute(request, "User1"), otroHilo);
      future.get();
    } finally {
      otroHilo.shutdown();
    }

    assertEquals("User1", usernameVistoPorElDelegate[0]);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }
}
