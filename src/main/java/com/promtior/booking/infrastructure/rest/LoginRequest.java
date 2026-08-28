package com.promtior.booking.infrastructure.rest;

import jakarta.validation.constraints.NotBlank;

record LoginRequest(@NotBlank String username, @NotBlank String password) {}
