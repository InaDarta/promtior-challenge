package com.promtior.booking.infrastructure.llm.failover;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Equivalente en streaming de {@link FailoverChatModel}: delega en {@code primary} y, ante un error
 * transitorio (ver {@link TransientLlmErrors}), pasa a {@code fallback}.
 *
 * <p>A diferencia del caso síncrono, el failover solo es seguro <b>antes</b> de haber emitido algún
 * token parcial: una vez que el cliente ya recibió texto del primario, reintentar contra el
 * respaldo produciría una respuesta con tokens duplicados o intercalados de dos proveedores
 * distintos. Si el error llega después del primer token, se propaga tal cual -- degradación
 * aceptable frente a una respuesta corrupta.
 */
public class FailoverStreamingChatModel implements StreamingChatModel {

  private static final Logger log = LoggerFactory.getLogger(FailoverStreamingChatModel.class);

  private final StreamingChatModel primary;
  private final StreamingChatModel fallback;

  public FailoverStreamingChatModel(StreamingChatModel primary, StreamingChatModel fallback) {
    this.primary = primary;
    this.fallback = fallback;
  }

  @Override
  public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
    AtomicBoolean anyTokenEmitted = new AtomicBoolean(false);
    primary.doChat(request, new FailoverAwareHandler(request, handler, anyTokenEmitted));
  }

  private class FailoverAwareHandler implements StreamingChatResponseHandler {

    private final ChatRequest request;
    private final StreamingChatResponseHandler handler;
    private final AtomicBoolean anyTokenEmitted;

    FailoverAwareHandler(
        ChatRequest request, StreamingChatResponseHandler handler, AtomicBoolean anyTokenEmitted) {
      this.request = request;
      this.handler = handler;
      this.anyTokenEmitted = anyTokenEmitted;
    }

    @Override
    public void onPartialResponse(String partialResponse) {
      anyTokenEmitted.set(true);
      handler.onPartialResponse(partialResponse);
    }

    @Override
    public void onCompleteResponse(ChatResponse response) {
      handler.onCompleteResponse(response);
    }

    @Override
    public void onError(Throwable error) {
      if (anyTokenEmitted.get() || !TransientLlmErrors.isTransient(error)) {
        handler.onError(error);
        return;
      }
      log.warn("Proveedor primario no disponible, usando el de respaldo", error);
      fallback.doChat(request, handler);
    }
  }
}
