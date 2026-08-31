package com.promtior.booking.infrastructure.rest.controller;

import com.promtior.booking.infrastructure.rest.dto.LoginRequest;
import com.promtior.booking.infrastructure.rest.dto.LoginResponse;
import com.promtior.booking.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Único endpoint público de la API: valida credenciales y devuelve un JWT. */
@RestController
@RequestMapping("/api/auth")
class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
  }

  @PostMapping("/login")
  LoginResponse login(@Valid @RequestBody LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.username(), request.password()));
    return new LoginResponse(jwtService.generateToken(request.username()));
  }
}
