package com.promtior.booking.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Reenvia los deep links del router de React ("/login", "/chat") a index.html: sin esto, un refresh
 * del navegador en esas rutas es un GET real al servidor, que no tiene ningun recurso estatico con
 * ese nombre y responde 404 en vez de servir el SPA.
 */
@Configuration
class SpaConfig {

  @Bean
  WebMvcConfigurer spaViewControllers() {
    return new WebMvcConfigurer() {
      @Override
      public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("forward:/index.html");
        registry.addViewController("/chat").setViewName("forward:/index.html");
      }
    };
  }
}
