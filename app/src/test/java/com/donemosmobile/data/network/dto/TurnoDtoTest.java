package com.donemosmobile.data.network.dto;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link TurnoDto} display helpers.
 *
 * <p>These cover the pure formatting logic, so they run on the JVM without the
 * Android framework.
 */
public class TurnoDtoTest {

  @Test
  public void horaCortaReturnsEmptyWhenHoraMissing() {
    TurnoDto dto = new TurnoDto();
    assertEquals("", dto.horaCorta());

    dto.hora = "";
    assertEquals("", dto.horaCorta());
  }

  @Test
  public void horaCortaKeepsPlainTimeAsIs() {
    TurnoDto dto = new TurnoDto();
    dto.hora = "09:30";
    assertEquals("09:30", dto.horaCorta());
  }

  @Test
  public void horaCortaTrimsSecondsFromPlainTime() {
    TurnoDto dto = new TurnoDto();
    dto.hora = "09:30:00";
    assertEquals("09:30", dto.horaCorta());
  }

  @Test
  public void horaCortaDropsIsoDatePrefix() {
    TurnoDto dto = new TurnoDto();
    dto.hora = "2024-03-15T09:30:00";
    assertEquals("09:30", dto.horaCorta());
  }

  @Test
  public void horaCortaDropsLegacySpaceSeparator() {
    TurnoDto dto = new TurnoDto();
    dto.hora = "2024-03-15 09:30:00";
    assertEquals("09:30", dto.horaCorta());
  }

  @Test
  public void horaCortaStripsIsoTimezoneSuffix() {
    TurnoDto dto = new TurnoDto();
    dto.hora = "2024-03-15T09:30:00.000000Z";
    assertEquals("09:30", dto.horaCorta());
  }

  @Test
  public void horaCortaLeavesShortValueUntouched() {
    TurnoDto dto = new TurnoDto();
    dto.hora = "9:30";
    assertEquals("9:30", dto.horaCorta());
  }
}
