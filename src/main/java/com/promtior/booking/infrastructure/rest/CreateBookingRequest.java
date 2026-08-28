package com.promtior.booking.infrastructure.rest;

import com.promtior.booking.domain.Room;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

record CreateBookingRequest(
    @NotBlank String title,
    @Min(1) int attendeeCount,
    @NotNull Room room,
    @NotNull @Schema(example = "2026-08-31T10:00:00") LocalDateTime start,
    @NotNull @Schema(example = "2026-08-31T11:00:00") LocalDateTime end) {}
