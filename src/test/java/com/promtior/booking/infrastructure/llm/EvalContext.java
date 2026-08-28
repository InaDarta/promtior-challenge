package com.promtior.booking.infrastructure.llm;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Estado que un {@link EvalCase} arma antes de conversar y que sus turnos pueden necesitar para
 * redactar el mensaje -- hoy, solo el id de una reserva sembrada de antemano (por ejemplo, la
 * reserva ajena de un caso de suplantación, que el modelo no puede descubrir por su cuenta porque
 * {@code listMyBookings} nunca devuelve reservas de otro usuario).
 */
final class EvalContext {

  private final Map<String, UUID> idsSembrados = new HashMap<>();

  void registrarId(String clave, UUID id) {
    idsSembrados.put(clave, id);
  }

  UUID id(String clave) {
    UUID id = idsSembrados.get(clave);
    if (id == null) {
      throw new IllegalStateException(
          "el caso pide el id sembrado '%s' pero ningún setup lo registró".formatted(clave));
    }
    return id;
  }
}
