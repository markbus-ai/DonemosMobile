package com.donemosmobile.ui.turnos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.donemosmobile.data.network.dto.TurnoWarningResponse;
import com.donemosmobile.ui.ForceOverride;
import com.google.gson.Gson;

import java.util.List;

import org.junit.Test;

/** JVM tests for the 409 decision of POST /api/turnos. */
public class TurnoWarningPresenterTest {

  private static TurnoWarningResponse parse(String json) {
    return new Gson().fromJson(json, TurnoWarningResponse.class);
  }

  @Test
  public void hardLimitIsNotForcibleAndFlagsATakenSlot() {
    TurnoWarningResponse warning = new TurnoWarningResponse();
    warning.warning = true;
    warning.code = ForceOverride.HORARIO_OCUPADO;
    warning.message = "Horario ocupado";
    TurnoWarningPresenter.Decision decision = TurnoWarningPresenter.decide(warning);
    assertFalse(decision.forcible);
    assertTrue(decision.horarioOcupado);
    assertEquals("Horario ocupado", decision.text);
  }

  @Test
  public void emptyEnvelopeFallsBackToTheGenericMessage() {
    TurnoWarningPresenter.Decision decision =
        TurnoWarningPresenter.decide(parse("{\"warning\":true}"));
    assertEquals(List.of(TurnoWarningPresenter.FALLBACK_MESSAGE), decision.messages);
  }

  @Test
  public void severalMessagesAreJoinedOnePerLine() {
    TurnoWarningPresenter.Decision decision = TurnoWarningPresenter.decide(parse(
        "{\"warning\":true,\"warnings\":[{\"code\":\"A\",\"message\":\"Uno\"},"
            + "{\"code\":\"B\",\"message\":\"Dos\"}]}"));
    assertEquals("Uno\nDos", decision.text);
  }

  @Test
  public void donationRuleIsForcible() {
    TurnoWarningPresenter.Decision decision = TurnoWarningPresenter.decide(parse(
        "{\"warning\":true,\"code\":\"INTERVALO\",\"message\":\"Faltan días\"}"));
    assertTrue(decision.forcible);
    assertFalse(decision.horarioOcupado);
  }
}
