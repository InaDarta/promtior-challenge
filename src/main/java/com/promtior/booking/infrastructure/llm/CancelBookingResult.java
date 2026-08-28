package com.promtior.booking.infrastructure.llm;

/**
 * Resultado estructurado de {@link BookingTools#cancelBooking}: éxito, o el código de error para
 * que el modelo lo explique sin distinguir "no existe" de "es ajena" (ADR 0008).
 */
record CancelBookingResult(boolean success, String errorCode, String errorMessage) {

  static CancelBookingResult ok() {
    return new CancelBookingResult(true, null, null);
  }

  static CancelBookingResult error(String errorCode, String errorMessage) {
    return new CancelBookingResult(false, errorCode, errorMessage);
  }
}
