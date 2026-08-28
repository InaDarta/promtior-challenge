package com.promtior.booking.infrastructure.rest;

import jakarta.validation.constraints.NotBlank;

record ChatRequest(@NotBlank String message) {}
