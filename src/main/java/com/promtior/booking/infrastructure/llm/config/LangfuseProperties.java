package com.promtior.booking.infrastructure.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code app.observability.langfuse.enabled}: apaga toda la instrumentación de E07.4 -- spans de
 * generación y de tool calls -- cuando no hay Langfuse configurado. Ver {@code application.yml} y
 * ADR 0011.
 *
 * <p>No alcanza con deshabilitar solo el exportador de OTLP ({@code
 * management.otlp.tracing.export.enabled}): sin este flag, cada llamada al modelo y cada tool call
 * seguirían armando spans -- serializando mensajes, argumentos y resultados -- que nadie exporta ni
 * lee. {@code @DefaultValue} evita que el binding falle en los tests que arman un contexto de
 * Spring acotado (ver {@code ChatModelConfigTest}) sin fijar la property explícitamente.
 */
@ConfigurationProperties(prefix = "app.observability.langfuse")
public record LangfuseProperties(@DefaultValue("false") boolean enabled) {}
