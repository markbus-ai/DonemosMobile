package com.donemosmobile.data.donaciones;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.dto.DonacionWarningResponse;

import java.util.List;

import org.junit.Test;

/** JVM tests for the single 409 parsing path of POST /api/donaciones. */
public class DonacionWarningParsingTest {

  @Test
  public void parsesTheWarningsEnvelope() {
    DonacionWarningResponse warning = DonacionRepository.parseWarnings(ApiError.parse(
        "{\"warnings\":[{\"code\":\"INTERVALO\",\"message\":\"Faltan 10 días\"}]}"));
    assertEquals(List.of("Faltan 10 días"), warning.messages());
  }

  @Test
  public void bodyThatIsNotJsonIsNotAWarning() {
    assertNull(DonacionRepository.parseWarnings(ApiError.parse("<html>Conflict</html>")));
  }

  @Test
  public void missingBodyIsNotAWarning() {
    assertNull(DonacionRepository.parseWarnings(ApiError.parse(null)));
  }
}
