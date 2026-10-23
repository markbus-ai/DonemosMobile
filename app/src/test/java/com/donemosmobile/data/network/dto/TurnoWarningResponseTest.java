package com.donemosmobile.data.network.dto;

import static org.junit.Assert.assertEquals;

import com.google.gson.Gson;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

/** JVM tests for parsing the turnos 409 envelope. */
public class TurnoWarningResponseTest {

  private final Gson gson = new Gson();

  @Test
  public void collectsTopLevelAndNestedCodesWithoutDuplicates() {
    TurnoWarningResponse response = gson.fromJson("{\"warning\":true,\"code\":\"INTERVALO\","
        + "\"message\":\"Intervalo\",\"warnings\":[{\"code\":\"INTERVALO\",\"message\":\"Intervalo\"},"
        + "{\"code\":\"LIMITE\",\"message\":\"Límite anual\"}]}", TurnoWarningResponse.class);
    assertEquals(Arrays.asList("INTERVALO", "LIMITE"), response.codes());
    assertEquals(Arrays.asList("Intervalo", "Límite anual"), response.messages());
  }

  @Test
  public void fallsBackToTopLevelMessage() {
    TurnoWarningResponse response = gson.fromJson(
        "{\"warning\":true,\"code\":\"HORARIO_OCUPADO\",\"message\":\"Horario ocupado\"}",
        TurnoWarningResponse.class);
    assertEquals(Collections.singletonList("HORARIO_OCUPADO"), response.codes());
    assertEquals(Collections.singletonList("Horario ocupado"), response.messages());
  }
}
