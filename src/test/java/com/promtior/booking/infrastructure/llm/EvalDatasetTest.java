package com.promtior.booking.infrastructure.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Chequeo estructural del dataset de {@link EvalDataset}, sin ningún {@link
 * dev.langchain4j.model.chat.ChatModel} de por medio: corre en CI sin consumir ninguna API key (a
 * diferencia de {@link BookingAgentEvalRunner}, que sí necesita una y nunca corre ahí). Protege
 * contra que una edición futura del dataset lo deje con ids repetidos, sin las categorías
 * "difíciles" que pide el issue #43, o con un turno que ni siquiera arma una frase sin tirar.
 */
class EvalDatasetTest {

  @Test
  void elDatasetTieneVeinteCasosConIdsUnicos() {
    List<EvalCase> casos = EvalDataset.casos();
    assertEquals(20, casos.size());

    Set<String> ids = casos.stream().map(EvalCase::id).collect(Collectors.toSet());
    assertEquals(20, ids.size(), "hay ids de EvalCase repetidos");
  }

  @Test
  void cadaCasoTieneCategoriaCriterioYAlMenosUnTurnoNoVacio() {
    for (EvalCase caso : EvalDataset.casos()) {
      assertFalse(caso.categoria().isBlank(), caso.id() + ": categoria vacía");
      assertFalse(caso.criterioDeAcierto().isBlank(), caso.id() + ": criterioDeAcierto vacío");
      assertFalse(caso.turnos().isEmpty(), caso.id() + ": sin turnos");
    }
  }

  @Test
  void elDatasetCubreLasCuatroCategoriasQuePideElIssue() {
    Set<String> categorias =
        EvalDataset.casos().stream().map(EvalCase::categoria).collect(Collectors.toSet());
    assertTrue(categorias.contains("feliz"));
    assertTrue(categorias.contains("fecha relativa"));
    assertTrue(categorias.contains("dato faltante"));
    assertTrue(categorias.contains("pedido imposible"));
    assertTrue(categorias.contains("suplantación"));
  }

  @Test
  void elRelojFijoDeReferenciaEsUnLunesDentroDeHorarioDeOficina() {
    LocalDateTime ahora = LocalDateTime.now(EvalDataset.AHORA);
    assertEquals(DayOfWeek.MONDAY, ahora.getDayOfWeek());
    assertTrue(ahora.getHour() >= 8 && ahora.getHour() < 20);
  }

  @Test
  void losTurnosQueNoDependenDeUnIdSembradoArmanUnaFraseSinTirar() {
    EvalContext contextoVacio = new EvalContext();
    for (EvalCase caso : EvalDataset.casos()) {
      if (caso.id().equals("C18")) {
        continue; // depende del id que su propio setup siembra en tiempo de ejecución
      }
      for (var turno : caso.turnos()) {
        String frase = turno.apply(contextoVacio);
        assertFalse(frase.isBlank(), caso.id() + ": un turno armó una frase vacía");
      }
    }
  }
}
