package com.promtior.booking.infrastructure.rest.controller;

import com.promtior.booking.application.CancelBooking;
import com.promtior.booking.application.CreateBooking;
import com.promtior.booking.application.ListMyBookings;
import com.promtior.booking.domain.BookingRange;
import com.promtior.booking.infrastructure.rest.dto.BookingRanges;
import com.promtior.booking.infrastructure.rest.dto.BookingResponse;
import com.promtior.booking.infrastructure.rest.dto.CreateBookingRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Reservas del usuario autenticado: crear, listar las propias y cancelar. */
@RestController
@RequestMapping("/api/bookings")
@SecurityRequirement(name = "bearerAuth")
class BookingController {

  private final CreateBooking createBooking;
  private final ListMyBookings listMyBookings;
  private final CancelBooking cancelBooking;

  BookingController(
      CreateBooking createBooking, ListMyBookings listMyBookings, CancelBooking cancelBooking) {
    this.createBooking = createBooking;
    this.listMyBookings = listMyBookings;
    this.cancelBooking = cancelBooking;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
    BookingRange range = BookingRanges.of(request.start(), request.end());
    UUID id =
        createBooking.execute(request.title(), request.attendeeCount(), request.room(), range);
    return new BookingResponse(
        id,
        request.title(),
        request.attendeeCount(),
        request.room(),
        request.start(),
        request.end());
  }

  @GetMapping
  List<BookingResponse> myBookings() {
    return listMyBookings.execute().stream().map(BookingResponse::from).toList();
  }

  @DeleteMapping("/{id}")
  ResponseEntity<Void> cancel(@PathVariable UUID id) {
    cancelBooking.execute(id);
    return ResponseEntity.noContent().build();
  }
}
