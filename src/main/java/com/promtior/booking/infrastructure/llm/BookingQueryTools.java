package com.promtior.booking.infrastructure.llm;

import com.promtior.booking.application.ListMyBookings;
import dev.langchain4j.agent.tool.Tool;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Tool de consulta de reservas propias para {@link BookingAssistant}: adaptador fino sobre {@link
 * ListMyBookings}, sin lógica propia. El usuario se resuelve dentro del caso de uso vía {@code
 * CurrentUserProvider} (ADR 0007), nunca como parámetro de la tool. Habilita en la práctica
 * cancelar: sin ver sus reservas y el id de cada una, el usuario no tiene forma de referirse a cuál
 * cancelar.
 */
@Component
class BookingQueryTools {

  private final ListMyBookings listMyBookings;

  BookingQueryTools(ListMyBookings listMyBookings) {
    this.listMyBookings = Objects.requireNonNull(listMyBookings, "listMyBookings");
  }

  @Tool("Lista las reservas propias del usuario autenticado, con su id para poder cancelarlas")
  List<BookingSummary> listMyBookings() {
    return listMyBookings.execute().stream().map(BookingSummary::from).toList();
  }
}
