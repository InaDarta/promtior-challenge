package com.promtior.booking.infrastructure.llm.dto;

import com.promtior.booking.infrastructure.llm.tools.BookingTools;

/**
 * Resultado estructurado de {@link BookingTools#cancelBooking}: éxito, o el código de error para
 * que el modelo lo explique sin distinguir "no existe" de "es ajena" (ADR 0008).
 */
public record CancelBookingResult(boolean success, String errorCode, String errorMessage) {

  public static CancelBookingResult ok() {
    return new CancelBookingResult(true, null, null);
  }

  public static CancelBookingResult error(String errorCode, String errorMessage) {
    return new CancelBookingResult(false, errorCode, errorMessage);
  }
}
