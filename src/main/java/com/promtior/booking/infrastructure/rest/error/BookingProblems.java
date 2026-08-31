package com.promtior.booking.infrastructure.rest.error;

import com.promtior.booking.domain.BookingError;
import com.promtior.booking.infrastructure.rest.dto.TimeSlotResponse;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Traduce cada subtipo de {@link BookingError} a un {@link ProblemDetail}: un código estable por
 * tipo de violación (contrato de E04.4) más los datos de su causa, no solo el texto de {@link
 * BookingError#message()}.
 */
final class BookingProblems {

  private BookingProblems() {}

  static ProblemDetail from(BookingError error) {
    return switch (error) {
      case BookingError.CapacityExceeded e ->
          problem(
              HttpStatus.BAD_REQUEST,
              e,
              Map.of(
                  "room", e.room(),
                  "roomCapacity", e.room().capacity(),
                  "requestedAttendees", e.requestedAttendees()));
      case BookingError.SlotOccupied e ->
          problem(
              HttpStatus.CONFLICT,
              e,
              Map.of(
                  "room", e.room(),
                  "conflictingSlots",
                      e.conflictingSlots().stream().map(TimeSlotResponse::from).toList()));
      case BookingError.MaxDurationExceeded e ->
          problem(
              HttpStatus.BAD_REQUEST,
              e,
              Map.of(
                  "requestedSlotCount", e.requestedSlotCount(),
                  "maxSlotCount", e.maxSlotCount()));
      case BookingError.NonContiguousRange e ->
          problem(
              HttpStatus.BAD_REQUEST,
              e,
              Map.of(
                  "before", TimeSlotResponse.from(e.before()),
                  "after", TimeSlotResponse.from(e.after())));
      case BookingError.OutsideOfficeHours e ->
          problem(HttpStatus.BAD_REQUEST, e, Map.of("start", e.start(), "end", e.end()));
      case BookingError.MissingTitle e -> problem(HttpStatus.BAD_REQUEST, e, Map.of());
      case BookingError.InThePast e ->
          problem(HttpStatus.BAD_REQUEST, e, Map.of("start", e.start(), "now", e.now()));
    };
  }

  private static ProblemDetail problem(
      HttpStatus status, BookingError error, Map<String, Object> causeData) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, error.message());
    problem.setProperty("code", error.code());
    causeData.forEach(problem::setProperty);
    return problem;
  }
}
