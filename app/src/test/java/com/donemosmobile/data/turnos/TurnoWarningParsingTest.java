package com.donemosmobile.data.turnos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.dto.TurnoWarningResponse;

import java.util.List;

import org.junit.Test;

/** JVM tests for the single 409 parsing path of POST /api/turnos. */
public class TurnoWarningParsingTest {

  @Test
  public void ruleEnvelopeIsAWarning() {
    TurnoWarningResponse warning = TurnoRepository.parseWarning(ApiError.parse(
        "{\"warning\":true,\"code\":\"CAPACIDAD\",\"message\":\"Cupo completo\"}"));
    assertEquals(List.of("CAPACIDAD"), warning.codes());
  }

  @Test
  public void plainConflictMessageIsNotAWarning() {
    assertNull(TurnoRepository.parseWarning(ApiError.parse("{\"message\":\"Conflicto\"}")));
  }

  @Test
  public void bodyThatIsNotJsonIsNotAWarning() {
    assertNull(TurnoRepository.parseWarning(ApiError.parse("<html>Conflict</html>")));
  }
}
